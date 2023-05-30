import sbt._

object Dependencies {
  val zio           = "dev.zio" %% "zio"         % "2.0.10"
  val `zio-streams` = "dev.zio" %% "zio-streams" % "2.0.10"
  val `zio-kafka`   = "dev.zio" %% "zio-kafka"   % "2.3.1"

  val all: Seq[ModuleID] = Seq(zio, `zio-streams`, `zio-kafka`)
}
