package com.github.neofelis.exercise.web.actor

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.github.neofelis.exercise.web.message.OrderStore._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class OrderStoreTest extends TestKit(ActorSystem("testOrderStore"))
  with AnyWordSpecLike
  with Matchers
  with ImplicitSender {

  "The OrderStore Actor" must {
    "can get the order item until expiry" in {
      val tableName = UUID.randomUUID().toString
      val orderStoreActor = system.actorOf(props(tableName))

      val itemId = UUID.randomUUID().toString
      val menuId = 1
      val servingAt = System.currentTimeMillis() + 100

      orderStoreActor ! CreateOrderItem(OrderItem(itemId, menuId, servingAt))
      expectMsg(OrderCreated)

      orderStoreActor ! GetOrderItem(itemId)
      expectMsg(Some(OrderItem(itemId, menuId, servingAt)))

      Thread.sleep(100)

      orderStoreActor ! GetOrderItem(itemId)
      expectMsg(None)
    }

    "can list the order items until expiry" in {
      val tableName = UUID.randomUUID().toString
      val orderStoreActor = system.actorOf(props(tableName))

      val servingAt = System.currentTimeMillis() + 100
      val items = (1 to 10).map(n => OrderItem(UUID.randomUUID().toString, n, servingAt))

      orderStoreActor ! CreateOrderItems(items)
      expectMsg(OrderCreated)

      // TODO: Count the number of items
      orderStoreActor ! ListOrderItems()
      expectMsgType[Vector[OrderItem]]

      Thread.sleep(100)
      orderStoreActor ! ListOrderItems()
      expectMsg(Vector.empty)
    }

    "can delete the order item and can't get again" in {
      val tableName = UUID.randomUUID().toString
      val orderStoreActor = system.actorOf(props(tableName))

      val itemId = UUID.randomUUID().toString
      val menuId = 1
      val servingAt = System.currentTimeMillis() + 100

      orderStoreActor ! CreateOrderItem(OrderItem(itemId, menuId, servingAt))
      expectMsg(OrderCreated)

      orderStoreActor ! GetOrderItem(itemId)
      expectMsg(Some(OrderItem(itemId, menuId, servingAt)))

      orderStoreActor ! DeleteOrderItem(itemId)
      expectMsg(Some(OrderItem(itemId, menuId, servingAt)))

      orderStoreActor ! GetOrderItem(itemId)
      expectMsg(None)
    }
  }
}
