package com.github.neofelis.exercise.web.route

import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.MalformedRequestContentRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.neofelis.exercise.service.TrieMapOrderService
import com.github.neofelis.exercise.web.message.OrderItemMessage._
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContextExecutor

class OrderItemRouteTest extends OrderItemRoute with AnyWordSpecLike with Matchers with ScalaFutures with ScalatestRouteTest {
  override implicit val orderService: TrieMapOrderService[Int, String, OrderItem] = new TrieMapOrderService[Int, String, OrderItem]()
  override implicit val executionContextExecutor: ExecutionContextExecutor = system.dispatcher
  override implicit val config: Config = ConfigFactory
    .load("application.conf")
    .getConfig("exercise.restaurant")
    .getConfig("server")

  val validRequestOrder = """{"items":[{"tableId":1, "menuId":1},{"tableId":1, "menuId":2}]}"""
  val invalidRequestOrder = """{"items":[{"menuId":1},{"menuId":2}]}"""

  "Order Item Route" should {
    "return created for POST valid request to the orders path" in {
      val e = HttpEntity(MediaTypes.`application/json`, validRequestOrder)
      val request = Post("/order").withEntity(e)
      request ~> route ~> check {
        status shouldEqual StatusCodes.Created
        responseAs[String] shouldEqual "order created"
      }
    }

    "reject for POST invalid request to the orders path" in {
      val e = HttpEntity(MediaTypes.`application/json`, invalidRequestOrder)
      val request = Post("/order").withEntity(e)
      request ~> route ~> check {
        rejection shouldBe a[MalformedRequestContentRejection]
      }
    }

    "return OrderItem list for GET request to table path with some items" in {
      val orderItem = OrderItem(UUID.randomUUID().toString, 3, 1, System.currentTimeMillis() + 10000)
      orderService.create(orderItem.tableId, orderItem.id, orderItem, orderItem.servingAt)
      val request = Get("/order/3")
      request ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"items":[{"id":"${orderItem.id}","menuId":1,"servingAt":${orderItem.servingAt},"tableId":3}]}"""
      }
    }

    "return NotFound for GET request to the table path with no items" in {
      val request = Get("/order/4")
      request ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "return OrderItem for GET request to the existing item path" in {
      val orderItem = OrderItem(UUID.randomUUID().toString, 1, 1, System.currentTimeMillis() + 10000)
      orderService.create(orderItem.tableId, orderItem.id, orderItem, orderItem.servingAt)
      val request = Get(s"/order/1/${orderItem.id}")
      request ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"id":"${orderItem.id}","menuId":1,"servingAt":${orderItem.servingAt},"tableId":1}"""
      }
    }

    "return NotFound for GET request to the non-existing item path" in {
      val request = Get("/order/1/no-such-an-item")
      request ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "return OrderItem for DELETE request to the existing item path" in {
      val orderItem = OrderItem(UUID.randomUUID().toString, 1, 1, System.currentTimeMillis() + 10000)
      orderService.create(orderItem.tableId, orderItem.id, orderItem, orderItem.servingAt)
      val request = Delete(s"/order/1/${orderItem.id}")
      request ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"id":"${orderItem.id}","menuId":1,"servingAt":${orderItem.servingAt},"tableId":1}"""
      }
    }

    "return NotFound for DELETE request to the non-existing item path" in {
      val request = Delete("/order/1/no-such-an-item")
      request ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
}
