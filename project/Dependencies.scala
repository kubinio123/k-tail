import sbt._

object Dependencies {
  val zio                       = "dev.zio"                     %% "zio"                     % "2.0.10"
  val `zio-streams`             = "dev.zio"                     %% "zio-streams"             % "2.0.10"
  val `zio-kafka`               = "dev.zio"                     %% "zio-kafka"               % "2.3.1"
  val `zio-json`                = "dev.zio"                     %% "zio-json"                % "0.5.0"
  val `zio-config`              = "dev.zio"                     %% "zio-config"              % "4.0.0-RC16"
  val `zio-config-typesafe`     = "dev.zio"                     %% "zio-config-typesafe"     % "4.0.0-RC16"
  val `zio-config-magnolia`     = "dev.zio"                     %% "zio-config-magnolia"     % "4.0.0-RC16"
  val `tapir-core`              = "com.softwaremill.sttp.tapir" %% "tapir-core"              % "1.4.0"
  val `tapir-zio`               = "com.softwaremill.sttp.tapir" %% "tapir-zio"               % "1.4.0"
  val `tapir-http4s-server-zio` = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server-zio" % "1.4.0"
  val `http4s-blaze-server`     = "org.http4s"                  %% "http4s-blaze-server"     % "0.23.14"

  val all: Seq[ModuleID] = Seq(
    zio,
    `zio-streams`,
    `zio-kafka`,
    `zio-json`,
    `zio-config`,
    `zio-config-typesafe`,
    `zio-config-magnolia`,
    `tapir-core`,
    `tapir-zio`,
    `tapir-http4s-server-zio`,
    `http4s-blaze-server`
  )
}
