package com.github.neofelis.exercise.web.route

import java.util.UUID.randomUUID

import akka.Done
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.neofelis.exercise.service.OrderService
import com.github.neofelis.exercise.web.message.OrderItemMarshaller
import com.github.neofelis.exercise.web.message.OrderItemMessage._
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Random

trait OrderItemRoute extends OrderItemApi {

  protected val createOrderRoute: Route = {
    path("order") {
      post {
        entity(as[RequestOrder]) { requestOrder =>
          val saved: Future[Done] = createOrder(requestOrder)
          onComplete(saved) {
            Done => complete("order created")
          }
        }
      }
    }
  }

  protected val listItemsRoute: Route = {
    path("order" / IntNumber) { tableId =>
      get {
        onSuccess(listTableOrder(tableId)) {
          case Some(items) => complete(ResponseOrder(items))
          case None => complete(StatusCodes.NotFound)
        }
      }
    }
  }

  protected val getItemRoute: Route = {
    pathPrefix("order" / IntNumber) { tableId =>
      path(Segment) { itemId =>
        get {
          onSuccess(getItem(tableId, itemId)) {
            case Some(item) => complete(item)
            case None => complete(StatusCodes.NotFound)
          }
        }
      }
    }
  }

  protected val deleteItemRoute: Route = {
    pathPrefix("order" / IntNumber) { tableId =>
      path(Segment) { itemId =>
        delete {
          onSuccess(deleteItem(tableId, itemId)) {
            case Some(item) => complete(item)
            case None => complete(StatusCodes.NotFound)
          }
        }
      }
    }
  }

  val route: Route = createOrderRoute ~ listItemsRoute ~ getItemRoute ~ deleteItemRoute

}


trait OrderItemApi extends OrderItemMarshaller {
  implicit val orderService: OrderService[Int, String, OrderItem]

  implicit val executionContextExecutor: ExecutionContextExecutor

  implicit val config: Config

  // async apis
  def createOrder(order: RequestOrder): Future[Done] = {
    order match {
      case RequestOrder(items) =>
        orderService.create(items
          .map(mapToOrderItem)
          .map(orderItem => (orderItem.tableId, orderItem.id, orderItem, orderItem.servingAt))
          .toArray)
      case _ =>
    }

    Future {
      Done
    }
  }

  def listTableOrder(tableId: Int): Future[Option[Array[OrderItem]]] = Future {
    orderService.list(tableId)
  }

  def getItem(tableId: Int, itemId: String): Future[Option[OrderItem]] = Future {
    orderService.get(tableId, itemId)
  }

  def deleteItem(tableId: Int, itemId: String): Future[Option[OrderItem]] = Future {
    orderService.delete(tableId, itemId)
  }

  private def mapToOrderItem(requestItem: RequestItem): OrderItem =
    OrderItem(randomUUID().toString, requestItem.tableId, requestItem.menuId, servingTime())

  private def servingTime(): Long = System.currentTimeMillis() + Random.between(minServingTime, maxServingTime) * 1000

  private def minServingTime: Int = config.getInt("min-serving-seconds")

  private def maxServingTime: Int = config.getInt("max-serving-seconds")

}