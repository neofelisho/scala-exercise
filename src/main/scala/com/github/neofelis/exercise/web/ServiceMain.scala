package com.github.neofelis.exercise.web

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.github.neofelis.exercise.service.OrderService
import com.github.neofelis.exercise.web.message.OrderItemMessage.OrderItem
import com.github.neofelis.exercise.web.route.OrderItemRoute
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object ServiceMain extends App with OrderItemRoute {

  // needed to run the route
  implicit val system: ActorSystem = ActorSystem()
  // needed for the future map/flatmap in the end and future in async apis
  override implicit val executionContextExecutor: ExecutionContextExecutor = system.dispatcher
  // the store of table orders
  override implicit val orderService: OrderService[Int, String, OrderItem] = new OrderService[Int, String, OrderItem]()
  // configuration
  override implicit val config: Config = ConfigFactory
    .load("application.conf")
    .getConfig("exercise.restaurant")
    .getConfig("server")

  // server entry point
  val bindingFuture = Http().bindAndHandle(route, host, 8080)
  println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

  private def host: String = config.getString("host")

  private def port: Int = config.getInt("port")

}