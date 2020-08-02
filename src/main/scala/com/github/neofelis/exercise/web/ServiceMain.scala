package com.github.neofelis.exercise.web

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.util.Timeout
import com.github.neofelis.exercise.web.route.RestApi

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.io.StdIn

object ServiceMain extends App with RequestTimeout {
  // needed to run the route
  implicit val system: ActorSystem = ActorSystem()
  // needed for the future map/flatmap in the end and future in async apis
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val api = new RestApi(system, requestTimeout())

  val bindingFuture = Http().bindAndHandle(api.route, "localhost", 8080)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
}

trait RequestTimeout {
  def requestTimeout(): Timeout = {
    val d = Duration(100, TimeUnit.HOURS)
    FiniteDuration(d.length, d.unit)
  }
}