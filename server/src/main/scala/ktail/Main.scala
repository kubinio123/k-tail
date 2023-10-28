package ktail

import zio.*
import zio.config.*
import zio.config.typesafe.*
import zio.stream.ZSink

object Main extends ZIOAppDefault {

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.setConfigProvider(ConfigProvider.fromResourcePath())

  def program(config: TaskLayer[KTailConfig]): Task[ExitCode] =
    (for {
      _     <- KTailConsumer.consume.fork
      serve <- Server.serve
    } yield serve).provide(
      config,
      KTailConsumerImpl.live,
      BufferImpl.live,
      BroadcastImpl.live,
      ServerImpl.live
    )

  override def run: Task[ExitCode] = program(KTailConfig.live)
}
