package ktail

import zio.*
import zio.config.*
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.*

case class KTailConfig(port: Int, bootstrapServers: List[String], groupId: String, topics: List[String])

object KTailConfig {
  private val config: Config[KTailConfig] = deriveConfig[KTailConfig].nested("k-tail")

  val live: Layer[Config.Error, KTailConfig] =
    ZLayer.fromZIO(
      ZIO.config[KTailConfig](config).tap { config =>
        ZIO.logInfo(s"""
             |k-tail server configuration:
             |port: ${config.port}
             |bootstrap-servers: ${config.bootstrapServers.mkString(",")}
             |group-id: ${config.groupId}
             |topics: ${config.topics.mkString(",")}
             |""".stripMargin)
      }
    )
}
