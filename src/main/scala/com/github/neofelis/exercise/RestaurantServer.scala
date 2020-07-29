package com.github.neofelis.exercise

import java.util.UUID.randomUUID

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.util.Random

object RestaurantServer extends App {
  // needed to run the route
  implicit val system: ActorSystem = ActorSystem()
  // needed for the future map/flatmap in the end and future in async apis
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // the store of table orders
  private val orderService = new OrderService[Int, String, ResponseItem]()

  // domain model
  final case class RequestItem(tableId: Int, menuId: Int)

  final case class ResponseItem(id: String, tableId: Int, menuId: Int, expectedServingAt: Long)

  final case class RequestOrder(items: List[RequestItem])

  final case class ResponseOrder(items: List[ResponseItem])

  // formats for unmarshalling and marshalling
  implicit val requestItemFormat: RootJsonFormat[RequestItem] = jsonFormat2(RequestItem)
  implicit val responseItemFormat: RootJsonFormat[ResponseItem] = jsonFormat4(ResponseItem)
  implicit val requestOrderFormat: RootJsonFormat[RequestOrder] = jsonFormat1(RequestOrder)
  implicit val responseOrderFormat: RootJsonFormat[ResponseOrder] = jsonFormat1(ResponseOrder)

  // async apis
  def saveOrder(order: RequestOrder): Future[Done] = {
    order match {
      case RequestOrder(items) => items.foreach(item => {
        val resp = ResponseItem(randomUUID().toString, item.tableId, item.menuId, System.currentTimeMillis() + servingTime())
        orderService.create(resp.tableId, resp.id, resp, resp.expectedServingAt)
      })
      case _ =>
    }

    Future {
      Done
    }
  }

  def listTableOrder(tableId: Int): Future[Option[Array[ResponseItem]]] = Future {
    orderService.list(tableId)
  }

  def getItem(tableId: Int, itemId: String): Future[Option[ResponseItem]] = Future {
    orderService.get(tableId, itemId)
  }

  def deleteItem(tableId: Int, itemId: String): Future[Option[ResponseItem]] = Future {
    orderService.delete(tableId, itemId)
  }

  // route definition
  val route: Route =
    concat(
      pathPrefix("order" / IntNumber) {
        tableId =>
          concat(
            pathEnd {
              get {
                // there might be no item for a given id
                val maybeList: Future[Option[Array[ResponseItem]]] = listTableOrder(tableId)

                onSuccess(maybeList) {
                  case Some(items) => complete(items)
                  case None => complete(StatusCodes.NotFound)
                }
              }
            },
            path(Remaining) {
              itemId =>
                concat(
                  get {
                    val maybeGet: Future[Option[ResponseItem]] = getItem(tableId, itemId)

                    onSuccess(maybeGet) {
                      case Some(item) => complete(item)
                      case None => complete(StatusCodes.NotFound)
                    }
                  },
                  delete {
                    val maybeDelete: Future[Option[ResponseItem]] = deleteItem(tableId, itemId)

                    onSuccess(maybeDelete) {
                      case Some(item) => complete(item)
                      case None => complete(StatusCodes.NotFound)
                    }
                  }
                )
            }
          )
      },
      post {
        path("order") {
          entity(as[RequestOrder]) {
            order =>
              val saved: Future[Done] = saveOrder(order)
              onComplete(saved) {
                done =>
                  complete("order created")
              }
          }
        }
      }
    )

  // server entry point
  val bindingFuture = Http().bindAndHandle(route, host, 8080)
  println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

  // util
  private def servingTime(): Long = Random.between(minServingTime, maxServingTime) * 1000

  // configurations
  private def config = ConfigFactory
    .load("application.conf")
    .getConfig("exercise.restaurant")
    .getConfig("server")

  private def host: String = config.getString("host")

  private def port: Int = config.getInt("port")

  private def minServingTime: Int = config.getInt("min-serving-seconds")

  private def maxServingTime: Int = config.getInt("max-serving-seconds")
}
