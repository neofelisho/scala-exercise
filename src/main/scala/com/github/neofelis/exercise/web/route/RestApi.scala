package com.github.neofelis.exercise.web.route

import java.util.UUID.randomUUID

import akka.Done
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.neofelis.exercise.service.TrieMapOrderService
import com.github.neofelis.exercise.web.message.OrderItemMarshaller
import com.github.neofelis.exercise.web.message.OrderItemMessage._
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Random

trait OrderItemRoute extends OrderItemApi {

  /**
   * The endpoint for creating new order
   * POST /order
   */
  protected val createOrderRoute: Route = {
    path("order") {
      post {
        entity(as[RequestOrder]) { requestOrder =>
          val saved: Future[Done] = createOrder(requestOrder)
          onComplete(saved) {
            Done => complete(StatusCodes.Created, "order created")
          }
        }
      }
    }
  }

  /**
   * The endpoint for listing the order items of specific table
   * GET /order/:table_id
   */
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

  /**
   * The endpoint for getting the detail of specific order item
   * GET /order/:table_id/:item_id
   */
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

  /**
   * The endpoint for deleting a specific order item
   * DELETE /order/:table_id/:item_id
   */
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

  /**
   * All routes of Order Item API
   */
  val route: Route = createOrderRoute ~ listItemsRoute ~ getItemRoute ~ deleteItemRoute

}

trait OrderItemApi extends OrderItemMarshaller {
  implicit val orderService: TrieMapOrderService[Int, String, OrderItem]

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

  private def servingTime(): Long = System.currentTimeMillis() + (minServingTime + Random.nextInt(maxServingTime + 1)) * 1000

  private def minServingTime: Int = config.getInt("min-serving-seconds")

  private def maxServingTime: Int = config.getInt("max-serving-seconds")

}