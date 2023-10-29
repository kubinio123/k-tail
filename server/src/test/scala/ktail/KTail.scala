package ktail

import sttp.client3.*
import sttp.client3.httpclient.zio.SttpClient
import sttp.model.StatusCode
import zio.*

object KTail {

  val live: RLayer[KTailConfig & SttpClient, Unit] =
    ZLayer.scoped {
      ZIO
        .acquireRelease(
          for {
            config <- ZIO.service[KTailConfig]
            sttp   <- ZIO.service[SttpClient]
            program <-
              Main.program(config = ZLayer.succeed(config)).forkScoped.interruptible
            _ <-
              sttp
                .send(
                  basicRequest
                    .get(uri"http://localhost:${config.port}/k-tail/health")
                    .response(asString)
                )
                .catchAll(_ => ZIO.succeed(ServiceUnavailable))
                .repeatUntil(_.code.isSuccess)
            _ <- ZIO.logInfo(s"k-tail started on port ${config.port}")
          } yield program
        )(program => program.interrupt *> ZIO.logInfo("k-tail stopped"))
        .unit
    }

  private val ServiceUnavailable: Response[Either[String, String]] =
    Response(Left("k-tail unavailable"), StatusCode.ServiceUnavailable)
}
