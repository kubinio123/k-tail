package ktail

import com.dimafeng.testcontainers.KafkaContainer
import zio.*

object TestKTailConfig {

  val live: URLayer[KafkaContainer, KTailConfig] =
    ZLayer {
      for {
        bootstrapServers <- ZIO.serviceWith[KafkaContainer](_.bootstrapServers.split(',').toList)
        random           <- ZIO.random
        port             <- random.nextIntBetween(9000, 9999)
        config = KTailConfig(
          port = port,
          bootstrapServers = bootstrapServers,
          groupId = "k-tail-server-test",
          topics = List("test-topic-1", "test-topic-2")
        )
      } yield config
    }

}
