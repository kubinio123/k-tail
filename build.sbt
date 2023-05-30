ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

ThisBuild / scalacOptions ++=
  Seq(
    "-source:future",
    "-deprecation"
  )

lazy val server = (project in file("server"))
  .configs(IntegrationTest)
  .settings(
    name := "k-tail",
    libraryDependencies ++= Dependencies.all
  )

lazy val producer = (project in file("producer"))
  .settings(
    name := "producer",
    libraryDependencies ++= Dependencies.all
  )
