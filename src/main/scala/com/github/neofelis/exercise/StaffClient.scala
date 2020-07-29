package com.github.neofelis.exercise

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import spray.json.DefaultJsonProtocol._
import spray.json.{RootJsonFormat, _}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Random, Success}

object StaffClient extends App {
  // needed to run the client
  implicit val system: ActorSystem = ActorSystem()
  // needed for the future in createOrderRequest
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // domain model
  case class Item(tableId: Int, menuId: Int)

  case class Order(items: List[Item])

  // formats for marshalling
  implicit val requestItemFormat: RootJsonFormat[Item] = jsonFormat2(Item)
  implicit val responseItemFormat: RootJsonFormat[Order] = jsonFormat1(Order)

  val poolClientFlow = Http().cachedHostConnectionPool[Order](apiHost, apiPort)

  def createOrderRequest(orderToCreate: Order): Future[(HttpRequest, Order)] = Future {
    val e = HttpEntity(MediaTypes.`application/json`, orderToCreate.toJson.toString())
    HttpRequest(method = HttpMethods.POST, uri = apiUri, entity = e) -> orderToCreate
  }

  // order generator
  def orderSource(): Source[Order, NotUsed] = {
    Source(
      (1 to orderCount).map(n => {
        val tableId = n % tableCount + 1
        Order(
          (1 to itemCount).map(_ =>
            Item(tableId, menuId())
          ).toList
        )
      })
    )
  }

  // request sender
  orderSource()
    .mapAsync(parallelism)(createOrderRequest)
    .via(poolClientFlow)
    .runForeach {
      case (Success(response), createdOrder) =>
        println(s"order: $createdOrder created: $response")
        response.discardEntityBytes()
      case (Failure(ex), failedOrder) =>
        println(s"order: $failedOrder failed: $ex")
    }

  // util
  private def menuId(): Int = Random.between(1, menuCount)

  // configuration
  private def config = ConfigFactory
    .load("application.conf")
    .getConfig("exercise.restaurant")
    .getConfig("client")

  private def apiHost: String = config.getString("api-host")

  private def apiPort: Int = config.getInt("api-port")

  private def apiOrderResource: String = config.getString("api-order-resource")

  private def apiUri: String = s"http://$apiHost:$apiPort/$apiOrderResource"

  private def parallelism: Int = config.getInt("parallelism")

  private def tableCount: Int = config.getInt("table-count")

  private def menuCount: Int = config.getInt("menu-count")

  private def orderCount: Int = config.getInt("order-count")

  private def itemCount: Int = config.getInt("item-count-in-order")
}
