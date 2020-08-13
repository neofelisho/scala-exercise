package com.github.neofelis.exercise.performance

import java.util.UUID

import com.github.neofelis.exercise.model._
import com.github.neofelis.exercise.web.message.OrderItemMessage.OrderItem

import scala.collection.parallel.CollectionConverters._
import scala.collection.parallel.ForkJoinTaskSupport
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object MapPerformanceTest extends App with MyTimer {

  val hashMap = new HashMapWithTTL[String, OrderItem]()
  val trieMap = new TrieMapWithTTL[String, OrderItem]()

  val items = (1 to 10_000).par.map(n => {
    OrderItem(UUID.randomUUID().toString, n, n, System.currentTimeMillis() + 10_000)
  })

  val forkJoinPool = new java.util.concurrent.ForkJoinPool(100)
  items.tasksupport = new ForkJoinTaskSupport(forkJoinPool)

  val testTrieMap = Future {
    timer("TrieMap", items.foreach(item => {
      trieMap.create(item.id, item, System.currentTimeMillis() + 10_000)
      trieMap.listValue()
    }))
  }

  val testHashMap = Future {
    timer("HashMap", items.foreach(item => {
      hashMap.create(item.id, item, System.currentTimeMillis() + 10_000)
      hashMap.listValue()
    }))
  }

  Await.result(testTrieMap, Duration.Inf)
  Await.result(testHashMap, Duration.Inf)
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