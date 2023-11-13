package ktail

import zio.*
import zio.kafka.consumer.Subscription.Topics
import zio.kafka.consumer.{ Consumer, ConsumerSettings }
import zio.kafka.serde.Serde
import zio.stream.*

trait KTailConsumer {
  // ZStream[Any, Throwable, Message]
  val consume: Stream[Throwable, Message]
}

object KTailConsumer {
  val consume: URIO[KTailConsumer, Stream[Throwable, Message]] =
    ZIO.serviceWith[KTailConsumer](_.consume)
}

final case class KTailConsumerImpl(topics: Set[String], consumer: Consumer) extends KTailConsumer {
  override val consume: Stream[Throwable, Message] =
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
}

object KTailConsumerImpl {
  // ZLayer[KTailConfig, Throwable, KTailConsumer]
  val live: RLayer[KTailConfig, KTailConsumer] =
    ZLayer.scoped {
      for {
        config <- ZIO.service[KTailConfig]
        topics = config.topics.toSet
        consumer <- Consumer.make(
          ConsumerSettings(config.bootstrapServers)
            .withGroupId(config.groupId)
        )
      } yield KTailConsumerImpl(topics, consumer)
    }
}
