package ktail

import zio.*
import zio.config.*
import zio.config.typesafe.*
import zio.stream.ZSink

object Main extends ZIOAppDefault {

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.setConfigProvider(ConfigProvider.fromResourcePath())

  private val program: RIO[KTailConsumer & Buffer & Server, ExitCode] =
    for {
      consume <- KTailConsumer.consume
      _       <- consume.mapZIO(Buffer.offer).run(ZSink.drain).forkDaemon
      serve   <- Server.serve
    } yield serve

  override def run: Task[ExitCode] = program
    .provide(
      KTailConfig.live,
      BufferImpl.live,
      KTailConsumerImpl.live,
      ServerImpl.live,
      BroadcastImpl.live
    )
}
