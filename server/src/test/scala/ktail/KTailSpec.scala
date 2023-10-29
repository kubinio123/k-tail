package ktail

import com.dimafeng.testcontainers.KafkaContainer
import ktail.TestKTailConfig.{ Topic1, Topic2, Topic3 }
import sttp.client3.*
import sttp.client3.httpclient.zio.*
import sttp.ws.{ WebSocket, WebSocketFrame }
import zio.*
import zio.json.*
import zio.kafka.producer.{ Producer, ProducerSettings }
import zio.kafka.serde.Serde
import zio.test.*
import zio.test.Assertion.{ assertion, equalTo, forall }

object KTailSpec extends ZIOSpecDefault {

  override def spec =
    suite("k-tail socket")(
      test("pong on ping") {
        for {
          socket <- subscribe(Topic1)
          ping   <- ping(socket)
          pong   <- socket.receive()
          _      <- socket.close()
        } yield assertTrue {
          pong match
            case WebSocketFrame.Pong(payload) => payload sameElements ping.payload
            case _                            => false
        }
      },
      test("broadcast kafka messages, single client") {
        for {
          socket   <- subscribe(Topic1)
          _        <- produce(Topic1, numberOfMessages = 10)
          messages <- receive(socket, numberOfMessages = 10)
        } yield assert(messages.map(_.topic))(forall(equalTo(Topic1))) &&
          assertTrue(messages.map(_.offset) == (0L to 9))
      },
      test("broadcast kafka messages, multiple clients") {
        for {
          socketsTopic2  <- ZIO.foreachPar(1 to 100)(_ => subscribe(Topic2))
          socketsTopic3  <- ZIO.foreachPar(1 to 100)(_ => subscribe(Topic3))
          _              <- produce(Topic2, numberOfMessages = 200)
          _              <- produce(Topic3, numberOfMessages = 200)
          messagesTopic2 <- ZIO.foreachPar(socketsTopic2)(receive(_, numberOfMessages = 200))
          messagesTopic3 <- ZIO.foreachPar(socketsTopic3)(receive(_, numberOfMessages = 200))
        } yield matchTopic(messagesTopic2, Topic2) &&
        matchOffsets(messagesTopic2, 0L to 199) &&
        matchTopic(messagesTopic3, Topic3) &&
        matchOffsets(messagesTopic3, 0L to 199)
      }
    ).provideSomeShared[Scope](
      KTail.live,
      TestKTailConfig.live,
      TestKafkaContainer.live,
      TestProducer.live,
      HttpClientZioBackend.layer()
    ) @@ TestAspect.debug

  private def subscribe(topic: String): RIO[KTailConfig & SttpClient, WebSocket[Task]] =
    for {
      config <- ZIO.service[KTailConfig]
      sttp   <- ZIO.service[SttpClient]
      ws <- sttp
        .send(
          basicRequest
            .get(uri"ws://localhost:${config.port}/k-tail/$topic")
            .response(asWebSocketAlwaysUnsafe[Task])
        )
        .map(_.body)
      _ <- ping(ws) *> ws.receive()
    } yield ws

  private def ping(socket: WebSocket[Task]): Task[WebSocketFrame.Ping] =
    for {
      random <- ZIO.random
      ping   <- random.nextBytes(10).map(bytes => WebSocketFrame.Ping(bytes.toArray))
      _      <- socket.send(ping)
    } yield ping

  private def produce(topic: String, numberOfMessages: Int): RIO[Producer, Unit] =
    for {
      producer <- ZIO.service[Producer]
      _ <- producer.produce(topic, key = "1", value = "msg", Serde.string, Serde.string).repeatN(numberOfMessages - 1)
    } yield ()

  private def receive(socket: WebSocket[Task], numberOfMessages: Int): Task[List[Message]] =
    for {
      received <- Queue.unbounded[WebSocketFrame]
      _        <- socket.receive().flatMap(received.offer).repeatN(numberOfMessages - 1)
      _        <- socket.close()
      frames   <- received.takeAll
      messages <- decode(frames)
    } yield messages

  private def decode(frames: Chunk[WebSocketFrame]): Task[List[Message]] =
    ZIO.succeed {
      frames
        .collect { case WebSocketFrame.Text(payload, _, _) =>
          payload.fromJson[Message]
        }
        .collect { case Right(message) =>
          message
        }
        .toList
    }

  private def matchTopic(messages: Iterable[List[Message]], topic: String): TestResult =
    assert(messages.map(_.map(_.topic)))(forall(forall(equalTo(topic))))

  private def matchOffsets(messages: Iterable[List[Message]], offsets: Iterable[Long]): TestResult =
    assert(messages.map(_.map(_.offset)))(forall(equalTo(offsets)))
}
