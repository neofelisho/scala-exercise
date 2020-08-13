package com.github.neofelis.exercise.performance

import java.util.UUID
import java.util.concurrent.ForkJoinPool

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.github.neofelis.exercise.service.ActorOrderService.{CreateItem, ItemCreated, ListItems, props}
import com.github.neofelis.exercise.service.{HashMapOrderService, TrieMapOrderService}
import com.github.neofelis.exercise.web.message.OrderItemMessage.OrderItem

import scala.collection.parallel.CollectionConverters._
import scala.collection.parallel.ForkJoinTaskSupport
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Random

/**
 * 05:43:55.144 [testActorOrderService-akka.actor.default-dispatcher-6] INFO akka.event.slf4j.Slf4jLogger - Slf4jLogger started
 * HashMapOrderService started at 1597358635560
 * HashMapOrderService finished at 1597358663610
 * HashMapOrderService elapsed 28050ms
 * TrieMapOrderService started at 1597358635560
 * TrieMapOrderService finished at 1597358664019
 * TrieMapOrderService elapsed 28459ms
 * ActorOrderService started at 1597358635561
 * ActorOrderService finished at 1597358678491
 * ActorOrderService elapsed 42930ms
 * Check the total item counts for TrieMapOrderService: 100000
 * Check the total item counts for HashMapOrderService: 100000
 * Check the total item counts for ActorOrderService: 100000
 *
 * 05:45:59.206 [testActorOrderService-akka.actor.default-dispatcher-4] INFO akka.event.slf4j.Slf4jLogger - Slf4jLogger started
 * HashMapOrderService started at 1597358759630
 * HashMapOrderService finished at 1597358786603
 * HashMapOrderService elapsed 26973ms
 * TrieMapOrderService started at 1597358759630
 * TrieMapOrderService finished at 1597358788762
 * TrieMapOrderService elapsed 29132ms
 * ActorOrderService started at 1597358759631
 * ActorOrderService finished at 1597358799619
 * ActorOrderService elapsed 39988ms
 * Check the total item counts for TrieMapOrderService: 100000
 * Check the total item counts for HashMapOrderService: 100000
 * Check the total item counts for ActorOrderService: 100000
 */
object OrderServicePerformanceTest extends TestKit(ActorSystem("testActorOrderService")) with App with MyTimer with ImplicitSender {

  val trieMapOrderService = new TrieMapOrderService[Int, String, OrderItem]()
  val hashMapOrderService = new HashMapOrderService[Int, String, OrderItem]()
  val actorOrderService = system.actorOf(props[Int, String, OrderItem]())

  val items =
    (1 to 100_000)
      .map(n =>
        OrderItem(UUID.randomUUID().toString, n, n, System.currentTimeMillis() + 100_000_000))
      .map(item => (Random.between(1, 11), item.id, item, item.servingAt))
      .par

  val forkJoinPool = new ForkJoinPool(100)
  items.tasksupport = new ForkJoinTaskSupport(forkJoinPool)

  val testTrieMapOrderService = Future {
    timer("TrieMapOrderService", items.foreach(item => {
      trieMapOrderService.create(item._1, item._2, item._3, item._4)
      trieMapOrderService.list(item._1)
    }))
  }
  val testHashMapOrderService = Future {
    timer("HashMapOrderService", items.foreach(item => {
      hashMapOrderService.create(item._1, item._2, item._3, item._4)
      hashMapOrderService.list(item._1)
    }))
  }
  val testActorOrderService = Future {
    timer("ActorOrderService", items.foreach(item => {
      val probe = TestProbe()
      actorOrderService ! CreateItem(item._1, item._2, item._3, item._4, probe.ref)
      probe.expectMsg(ItemCreated)
      actorOrderService ! ListItems(item._1, probe.ref)
      probe.expectMsgType[Option[Vector[OrderItem]]]
    }))
  }

  Await.result(testTrieMapOrderService, Duration.Inf)
  Await.result(testHashMapOrderService, Duration.Inf)
  Await.result(testActorOrderService, Duration.Inf)

  def calNumberOfItems(fn: Int => Option[Array[OrderItem]]): Int = {
    (1 to 10)
      .map(tableId => {
        fn(tableId) match {
          case Some(value) => value.length
          case None => 0
        }
      })
      .sum
  }

  println(s"Check the total item counts for TrieMapOrderService: ${calNumberOfItems(trieMapOrderService.list)}")
  println(s"Check the total item counts for HashMapOrderService: ${calNumberOfItems(hashMapOrderService.list)}")
  println(s"Check the total item counts for ActorOrderService: ${calNumberOfItems(tableId => {
    actorOrderService ! ListItems(tableId, self)
    val result = expectMsgType[Option[Vector[OrderItem]]]
    result match {
      case Some(value) => Some(value.toArray)
      case None => None
    }
  })}")
}
