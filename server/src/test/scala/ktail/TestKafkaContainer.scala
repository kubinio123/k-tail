package ktail

import com.dimafeng.testcontainers.KafkaContainer
import zio.*

object TestKafkaContainer {

  val live: ULayer[KafkaContainer] =
    ZLayer.scoped {
      ZIO.acquireRelease(
        ZIO.attempt(KafkaContainer.Def().start()).orDie
      )(container =>
        ZIO.attempt(container.stop()).ignoreLogged
      )
    }
}
