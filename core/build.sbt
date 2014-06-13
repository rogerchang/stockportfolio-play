import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm

name := "core"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/")

libraryDependencies ++= {
  val akkaVersion = "2.3.3"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "com.typesafe.akka" %% "akka-agent" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
    "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
    "commons-io" % "commons-io" % "2.4" % "test",
    "com.github.nscala-time" %% "nscala-time" % "1.2.0",
    "com.novocode" % "junit-interface" % "0.10" % "test",
    "org.scalaz" %% "scalaz-core" % "7.0.6",
    "org.scalacheck" %% "scalacheck" % "1.11.4" % "test",
    "org.scalatest" % "scalatest_2.11" % "2.2.0" % "test")
}