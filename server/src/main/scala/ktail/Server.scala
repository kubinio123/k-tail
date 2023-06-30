package ktail

import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.http4s.server.websocket.WebSocketBuilder2
import sttp.capabilities
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.ztapir.*
import sttp.ws.WebSocketFrame
import zio.interop.catz.*
import zio.stream.{ Stream, ZStream }
import zio.{ ExitCode, Promise, Task, URLayer, ZIO, ZLayer }

import scala.concurrent.ExecutionContext

trait Server {
  val serve: Task[ExitCode]
}

object Server {
  val serve: ZIO[Server, Throwable, ExitCode] = ZIO.serviceWithZIO[Server](_.serve)
}

final case class ServerImpl(port: Int, broadcast: Broadcast, ec: ExecutionContext) extends Server {

  private val kTailSocket: ZServerEndpoint[Any, ZioStreams & WebSockets] =
    endpoint.get
      .in("k-tail" / path[String]("topic"))
      .out(webSocketBodyRaw(ZioStreams))
      .zServerLogic { topic =>
        ZIO.succeed { (in: Stream[Throwable, WebSocketFrame]) =>
          val out = for {
            isClosed <- Promise.make[Throwable, Unit]
            dequeue  <- broadcast.subscribe(topic)
            control = in.collectZIO {
              case WebSocketFrame.Ping(bytes) => ZIO.succeed(WebSocketFrame.Pong(bytes): WebSocketFrame)
              case close @ WebSocketFrame.Close(_, _) =>
                isClosed
                  .succeed(())
                  .zipLeft(broadcast.unsubscribe(topic))
                  .as(close: WebSocketFrame)
            }
            messages = ZStream.fromTQueue(dequeue).map(msg => WebSocketFrame.text(msg.toString): WebSocketFrame)
            frames   = messages.merge(control).interruptWhen(isClosed)
          } yield frames

          ZStream.unwrap(out)
        }
      }

  private val webSocketRoutes: WebSocketBuilder2[Task] => HttpRoutes[Task] =
    ZHttp4sServerInterpreter().fromWebSocket(kTailSocket).toRoutes

  override val serve: Task[ExitCode] =
    BlazeServerBuilder[Task]
      .withExecutionContext(ec)
      .bindHttp(port, "localhost")
      .withHttpWebSocketApp(wsb => Router("/" -> webSocketRoutes(wsb)).orNotFound)
      .serve
      .compile
      .drain
      .exitCode
}

object ServerImpl {
  val live: URLayer[KTailConfig & Broadcast, ServerImpl] =
    ZLayer {
      for {
        port      <- ZIO.serviceWith[KTailConfig](_.port)
        broadcast <- ZIO.service[Broadcast]
        executor  <- ZIO.executor
      } yield ServerImpl(port, broadcast, executor.asExecutionContext)
    }
}