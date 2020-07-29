name := "exercise"

version := "0.1"

scalaVersion := "2.13.3"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0" % "test"
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.14.1" % "test"
libraryDependencies += "org.scalatestplus" %% "scalacheck-1-14" % "3.2.0.0" % "test"
val AkkaVersion = "2.6.8"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % AkkaVersion
val AkkaHTTPVersion = "10.1.12"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % AkkaHTTPVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHTTPVersion
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"