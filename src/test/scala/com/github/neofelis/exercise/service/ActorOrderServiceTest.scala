package com.github.neofelis.exercise.service

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.github.neofelis.exercise.service.ActorOrderService._
import com.github.neofelis.exercise.web.message.OrderItemMessage.OrderItem
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

import scala.collection.parallel.CollectionConverters._
import scala.util.Random

class ActorOrderServiceTest extends TestKit(ActorSystem("testActorOrderService"))
  with AnyPropSpecLike
  with Matchers
  with ImplicitSender {

  property("should initialize with empty store") {
    val actor = system.actorOf(props[Int, String, OrderItem]())

    forAll(Gen.posNum[Int]) { tableId =>
      val probe = TestProbe()
      actor ! ListItems(tableId, probe.ref)
      probe.expectMsg(None)
    }
  }

  property("can add order items to same table in parallel") {
    forAll(Gen.posNum[Int]) { n =>
      val actor = system.actorOf(props[Int, String, OrderItem]())
      val tableId = 1 + Random.nextInt(10)
      (1 to n).par.foreach(x => {
        val probe = TestProbe()
        val item = OrderItem(UUID.randomUUID().toString, tableId, x, System.currentTimeMillis() + 10_000)
        actor ! CreateItem(tableId, item.id, item, item.servingAt, probe.ref)
        // need assertion whenever we send a message to actor, or it will cause the failure of subsequent assertion
        probe.expectMsg(ItemCreated)
      })
      val probe = TestProbe()
      actor ! ListItems(tableId, probe.ref)
      val msg = probe.expectMsgType[Some[Vector[OrderItem]]]
      msg.get.length shouldBe n
    }
  }

  property("can add order items to different tables in parallel") {
    forAll(Gen.posNum[Int]) { n =>
      val actor = system.actorOf(props[Int, String, OrderItem]())
      val genItems = (tableId: Int) => (1 to n).map(menuId => OrderItem(UUID.randomUUID().toString, tableId, menuId, System.currentTimeMillis() + 10_000))
      (1 to 10).par.foreach(tableId =>
        genItems(tableId)
          .par
          .foreach(item => {
            val probe = TestProbe()
            actor ! CreateItem(tableId, item.id, item, item.servingAt, probe.ref)
            probe.expectMsg(ItemCreated)
          })
      )
      (1 to 10).par.foreach(tableId => {
        val probe = TestProbe()
        actor ! ListItems(tableId, probe.ref)
        val msg = probe.expectMsgType[Some[Vector[OrderItem]]]
        msg.get.length shouldBe n
      })
    }
  }

  property("can add bulk items in parallel") {
    forAll(Gen.posNum[Int]) { numberOfList =>
      val actor = system.actorOf(props[Int, String, OrderItem]())
      val itemsList = (1 to numberOfList).map(_ =>
        (1 to 100).map(n => {
          val tableId = n / 10 + 1
          val menuId = numberOfList
          OrderItem(UUID.randomUUID().toString, tableId, menuId, System.currentTimeMillis() + 10_000)
        }))
      itemsList.par.foreach(items => {
        val input = items.map { x => (x.tableId, x.id, x, x.servingAt) }.toVector
        val probe = TestProbe()
        actor ! CreateItems(input, probe.ref)
        probe.expectMsg(ItemCreated)
      })

      val groups = itemsList.flatten.groupBy(item => item.tableId)
      groups.par.foreach(group => {
        val tableId = group._1
        val expectedItems = group._2
        // need to use probe and send the ref to Actor to test in parallel
        val probe = TestProbe()
        actor ! ListItems(tableId, probe.ref)
        val actualItems = probe.expectMsgType[Some[Vector[OrderItem]]]
        actualItems.get should contain theSameElementsAs expectedItems
      })
    }
  }

  property("can query the added item") {
    val actor = system.actorOf(props[Int, String, OrderItem]())
    forAll(Gen.posNum[Int]) { tableId =>
      val menuId = tableId
      val expectedItem = OrderItem(UUID.randomUUID().toString, tableId, menuId, System.currentTimeMillis() + 10_000)
      val probe = TestProbe()
      actor ! CreateItem(tableId, expectedItem.id, expectedItem, expectedItem.servingAt, probe.ref)
      probe.expectMsg(ItemCreated)
      actor ! GetItem(tableId, expectedItem.id, probe.ref)
      val actualItem = probe.expectMsgType[Some[OrderItem]]
      actualItem.get shouldBe expectedItem
    }
  }

  property("won't list expired items") {
    val actor = system.actorOf(props[Int, String, OrderItem]())
    forAll(Gen.posNum[Int]) { n =>
      val tableId = Random.between(1, 10)
      val expectedItems = (1 to n).map(menuId => OrderItem(UUID.randomUUID().toString, tableId, menuId, System.currentTimeMillis() + 100))
      val probe = TestProbe()
      actor ! CreateItems(expectedItems.map(item => (item.tableId, item.id, item, item.servingAt)).toVector, probe.ref)
      probe.expectMsg(ItemCreated)
      actor ! ListItems(tableId, probe.ref)
      val actualItems = probe.expectMsgType[Option[Vector[OrderItem]]]
      actualItems.get should contain theSameElementsAs expectedItems
      Thread.sleep(200)
      actor ! ListItems(tableId, probe.ref)
      val afterSleepItems = probe.expectMsgType[Option[Vector[OrderItem]]]
      afterSleepItems.get.length shouldBe 0
    }
  }

  property("can delete items") {
    forAll(Gen.posNum[Int]) { n =>
      val actor = system.actorOf(props[Int, String, OrderItem]())
      val tableId = Random.between(1, 100)
      val probe = TestProbe()
      actor ! ListItems(tableId, probe.ref)
      probe.expectMsg(None)
      val expectedItems = (1 to n).map(menuId => OrderItem(UUID.randomUUID().toString, tableId, menuId, System.currentTimeMillis() + 10_100))
      actor ! CreateItems(expectedItems.map(item => (tableId, item.id, item, item.servingAt)).toVector, probe.ref)
      probe.expectMsg(ItemCreated)
      actor ! ListItems(tableId, probe.ref)
      val actualItems = probe.expectMsgType[Some[Vector[OrderItem]]]
      actualItems.get should contain theSameElementsAs expectedItems
      expectedItems.par.foreach(expectedItem => {
        val deleteProbe = TestProbe()
        actor ! DeleteItem(tableId, expectedItem.id, deleteProbe.ref)
        val actualItem = deleteProbe.expectMsgType[Option[OrderItem]]
        actualItem.get shouldBe expectedItem
      })
      actor ! ListItems(tableId, probe.ref)
      val itemsAfterDelete = probe.expectMsgType[Some[Vector[OrderItem]]]
      itemsAfterDelete.get.length shouldBe 0
    }
  }

}
