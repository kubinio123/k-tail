package ktail

import zio.*
import zio.kafka.consumer.Subscription.Topics
import zio.kafka.consumer.{ Consumer, ConsumerSettings }
import zio.kafka.serde.Serde
import zio.stream.*

trait KTailConsumer {
  // ZIO[Any, Throwable, Unit]
  val consume: Task[Unit]
}

object KTailConsumer {
  val consume: URIO[KTailConsumer, Task[Unit]] =
    ZIO.serviceWith[KTailConsumer](_.consume)
}

final case class KTailConsumerImpl(topics: Set[String], consumer: Consumer, buffer: Buffer) extends KTailConsumer {
  override val consume: Task[Unit] =
    consumer
      .plainStream[Any, Array[Byte], Array[Byte]](Topics(topics), Serde.byteArray, Serde.byteArray)
      .map(record =>
        Message(
          record.offset.topicPartition.topic(),
          record.partition,
          record.offset.offset,
          record.key,
          record.value
        )
      )
      .mapZIO(buffer.offer)
      .run(ZSink.drain)
}

object KTailConsumerImpl {
  // ZLayer[KTailConfig & Buffer, Throwable, KTailConsumerImpl]
  val live: RLayer[KTailConfig & Buffer, KTailConsumerImpl] =
    ZLayer.scoped {
      for {
        config <- ZIO.service[KTailConfig]
        topics = config.topics.toSet
        consumer <- Consumer.make(
          ConsumerSettings(config.bootstrapServers)
            .withGroupId(config.groupId)
        )
        buffer <- ZIO.service[Buffer]
      } yield KTailConsumerImpl(topics, consumer, buffer)
    }
}
