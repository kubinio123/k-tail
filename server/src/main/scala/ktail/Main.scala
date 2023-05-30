package ktail

import zio.*
import zio.stream.ZSink

object Main extends ZIOAppDefault {

  private val program: RIO[Buffer & KTailConsumer, Unit] = for {
    consume <- KTailConsumer.consume
    _       <- consume.mapZIO(Buffer.offer).run(ZSink.drain)
  } yield ()

  override def run = program
    .provide(
      KTailConfig.live,
      BufferImpl.live,
      KTailConsumerImpl.live
    )
}
