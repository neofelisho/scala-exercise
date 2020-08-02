name := "exercise"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++= {
  val akkaVersion = "2.6.8"
  val akkaHTTPVersion = "10.1.12"
  Seq(
    "org.scalactic" %% "scalactic" % "3.2.0",
    "org.scalatest" %% "scalatest" % "3.2.0" % "test",
    "org.scalacheck" %% "scalacheck" % "1.14.1" % "test",
    "org.scalatestplus" %% "scalacheck-1-14" % "3.2.0.0" % "test",
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHTTPVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHTTPVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
  )
}
