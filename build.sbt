name := """stockportfolio-webapp"""

version := "1.0-SNAPSHOT"

lazy val webapp = (project in file("."))
  .enablePlugins(PlayScala)
  .aggregate(core)
  .dependsOn(core)

lazy val core = project

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)