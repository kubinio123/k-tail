package ktail

import zio.{ ULayer, ZLayer }

case class KTailConfig(port: Int, bootstrapServers: List[String], groupId: String, topics: List[String])

object KTailConfig {
  val live: ULayer[KTailConfig] =
    ZLayer.succeed(
      KTailConfig(
        port = 8080,
        bootstrapServers = List("localhost:9092"),
        groupId = "k-tail-server",
        topics = List("test-topic-1")
      )
    )
}
