package com.github.neofelis.exercise.web.actor

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.github.neofelis.exercise.web.message.OrderStore
import com.github.neofelis.exercise.web.message.Staff._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class StaffTest extends TestKit(ActorSystem("testStaff"))
  with AnyWordSpecLike
  with Matchers
  with ImplicitSender
  with DefaultTimeout {

  "Staff Actor" must {
    "can create new orders and list them from OrderStore" in {
      val staffActor = system.actorOf(props)

      val tableName = UUID.randomUUID().toString

      val items = (1 to 5).map(n => RequestItem(n))
      staffActor ! CreateOrder(RequestOrder(tableName, items))
      expectMsg(OrderStore.OrderCreated)

      val moreItems = (6 to 10).map(n => RequestItem(n))
      staffActor ! CreateOrder(RequestOrder(tableName, moreItems))
      expectMsg(OrderStore.OrderCreated)

      staffActor ! ListItems(tableName)
      expectMsgType[Vector[OrderStore.OrderItem]]
    }
  }
}
