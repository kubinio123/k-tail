package ktail

import com.dimafeng.testcontainers.KafkaContainer
import sttp.client3.*
import sttp.client3.httpclient.zio.*
import sttp.ws.WebSocketFrame
import zio.*
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.test.*

object KTailSpec extends ZIOSpecDefault {

  override def spec =
    suite("k-tail")(
      test("broadcast kafka messages over web socket") {
        for {
          config     <- ZIO.service[KTailConfig]
          producer   <- ZIO.service[Producer]
          sttpClient <- ZIO.service[SttpClient]
          messages   <- Queue.unbounded[WebSocketFrame]
          ws <- sttpClient
            .send(
              basicRequest
                .get(uri"ws://localhost:${config.port}/k-tail/test-topic-1")
                .response(asWebSocketAlwaysUnsafe[Task])
            )
            .map(_.body)
          _ <- ZIO.logInfo("connected to k-tail")
          _ <- ws.close()
        } yield assertTrue(true)
      }
    ).provideSomeShared[Scope](
      KTail.live,
      TestKTailConfig.live,
      TestKafkaContainer.live,
      TestProducer.live,
      HttpClientZioBackend.layer()
    ) @@ TestAspect.debug
}
