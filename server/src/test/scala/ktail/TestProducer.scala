package ktail

import com.dimafeng.testcontainers.KafkaContainer
import zio.*
import zio.kafka.producer.{ Producer, ProducerSettings }

object TestProducer {

  val live: RLayer[Scope & KTailConfig, Producer] =
    ZLayer {
      for {
        bootstrapServers <- ZIO.serviceWith[KTailConfig](_.bootstrapServers)
        producer         <- Producer.make(ProducerSettings(bootstrapServers))
      } yield producer
    }
}
