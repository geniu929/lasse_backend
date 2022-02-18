version := "1.0.0"
name := "openapi-scala-akka-http-server"
organization := "org.openapitools"
scalaVersion := "2.13.4"
lazy val AkkaHttpVersion = "10.2.1"
lazy val AkkaVersion = "2.6.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
)
