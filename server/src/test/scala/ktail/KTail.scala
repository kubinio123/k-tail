package ktail

import sttp.client3.*
import sttp.client3.httpclient.zio.SttpClient
import sttp.model.StatusCode
import zio.*

object KTail {

  val live: RLayer[Scope & KTailConfig & SttpClient, Unit] =
    ZLayer {
      ZIO
        .acquireRelease(
          for {
            config  <- ZIO.service[KTailConfig]
            sttp    <- ZIO.service[SttpClient]
            program <- Main.program(config = ZLayer.succeed(config)).forkScoped.interruptible
            _ <- sttp
              .send(basicRequest.get(uri"http://localhost:${config.port}/k-tail/health").response(asString))
              .catchAll(_ => ZIO.succeed(Response(Left("k-tail unavailable"), StatusCode.ServiceUnavailable)))
              .repeatUntil(_.code.isSuccess)
            _ <- ZIO.logInfo(s"k-tail started on port ${config.port}")
          } yield program
        )(_.interrupt)
        .unit
    }
}
