package com.github.neofelis.exercise.performance

import java.util.UUID

import com.github.neofelis.exercise.model._
import com.github.neofelis.exercise.web.message.OrderItemMessage.OrderItem

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object MapPerformanceTest extends App with MyTimer {
  val hashMap = new HashMapWithTTL[String, OrderItem]()
  val trieMap = new MapStoreWithTTL[String, OrderItem]()

  def testLoop(function: OrderItem => Option[OrderItem]): Unit = {
    (1 to 10000).par.foreach(n => {
      val item = OrderItem(UUID.randomUUID().toString, n, n, System.currentTimeMillis() + 10000)
      function(item)
    })
  }

  val testHashMap = Future {
    timer("HashMap", testLoop(s => hashMap.create(s.id, s, System.currentTimeMillis() + 10000)))
  }

  val testTrieMap = Future {
    timer("TrieMap", testLoop(s => trieMap.create(s.id, s, System.currentTimeMillis() + 10000)))
  }

  Await.result(testHashMap, Duration.Inf)
  Await.result(testTrieMap, Duration.Inf)
}

trait MyTimer {
  def timer[R](name: String, block: => R): R = {
    val start = System.currentTimeMillis()
    val result = block
    val end = System.currentTimeMillis()

    println(s"$name started at $start")
    println(s"$name finished at $end")
    println(s"$name elapsed ${end - start}ms")
    result
  }
}