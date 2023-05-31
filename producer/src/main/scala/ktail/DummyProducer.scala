package ktail

import org.apache.kafka.clients.producer.ProducerRecord
import zio.*
import zio.kafka.producer.*
import zio.kafka.serde.*

object DummyProducer extends ZIOAppDefault {

  private val producer: TaskLayer[Producer] =
    ZLayer.scoped(Producer.make(ProducerSettings(List("localhost:9092"))))

  private val program: RIO[Producer, Nothing] = {
    val topic  = "test-topic-1"
    val keys   = List("1", "2", "3", "4")
    val values = List("a", "b", "c", "d")

    val produce: RIO[Producer, Unit] = for {
      key   <- Random.shuffle(keys).map(_.head)
      value <- Random.shuffle(values).map(_.head)
      record = new ProducerRecord(topic, key, value)
      result <- Producer.produce(record, Serde.string, Serde.string)
      _      <- ZIO.log(s"produced message, offset ${result.offset()}")
      _      <- ZIO.sleep(1.second)
    } yield ()

    produce.forever
  }

  override def run: Task[Nothing] = program.provideLayer(producer)

}
