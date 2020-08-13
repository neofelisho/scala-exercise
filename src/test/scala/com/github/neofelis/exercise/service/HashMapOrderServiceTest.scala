package com.github.neofelis.exercise.service

import java.util.UUID

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpecLike
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.collection.parallel.CollectionConverters._
import scala.util.Random

class HashMapOrderServiceTest extends AnyPropSpecLike with Matchers with ScalaCheckDrivenPropertyChecks {
  property("should start with empty store") {
    val service = new HashMapOrderService[Int, Int, Long]()
    forAll(Gen.posNum[Int]) { n =>
      service.list(n) shouldBe None
    }
  }

  property("can add items to one group in parallel") {
    forAll(Gen.posNum[Int]) { n =>
      val service = new HashMapOrderService[Int, Int, Long]()
      val groupId = Random.between(1, 10)
      (1 to n).par.foreach(x => service.create(groupId, x, System.currentTimeMillis(), System.currentTimeMillis() + 10000))
      service.list(groupId).get.length shouldBe n
    }
  }

  property("can add items to different groups in parallel") {
    forAll(Gen.posNum[Int]) { n =>
      val service = new HashMapOrderService[Int, Int, Long]()
      (1 to n).par.foreach(x => service.create(x, x, System.currentTimeMillis(), System.currentTimeMillis() + 10000))
      (1 to n).par.foreach(x => service.list(x).get.length shouldBe 1)
    }
  }

  property("can add bulk items in parallel") {
    forAll(Gen.listOfN(10, Gen.posNum[Int])) { list =>
      val service = new HashMapOrderService[Int, String, Long]()
      list
        .zipWithIndex
        .map {
          case (n, idx) =>
            (1 to n).map(_ => (idx + 1, UUID.randomUUID().toString, System.currentTimeMillis(), System.currentTimeMillis() + 10000))
        }
        .foreach(x => service.create(x.toArray))

      (1 to 10).map(n => service.list(n).get.length).sum shouldBe list.sum
    }
  }

  property("can query the added item") {
    val service = new HashMapOrderService[Int, String, Long]()
    forAll(Gen.posNum[Int]) {
      tableId =>
        val itemId = UUID.randomUUID().toString
        service.create(tableId, itemId, System.currentTimeMillis(), System.currentTimeMillis() + 10000)
        service.get(tableId, itemId) shouldBe a[Some[_]]
    }
  }

  property("won't list expired items") {
    val service = new HashMapOrderService[Int, Int, Long]()
    (1 to 10).par.foreach(n => service.create(n, n, System.currentTimeMillis(), System.currentTimeMillis() + 100))
    (1 to 10).par.foreach(n => service.list(n).get.length shouldBe 1)
    Thread.sleep(200)
    (1 to 10).par.foreach(n => service.list(n).get.length shouldBe 0)
  }

  property("can delete items") {
    forAll(Gen.posNum[Int]) { n =>
      val service = new HashMapOrderService[Int, Int, Long]
      val tableId = 1 + Random.nextInt(10)
      service.list(tableId) shouldBe None
      (1 to n).par.foreach(x => service.create(tableId, x, System.currentTimeMillis(), System.currentTimeMillis() + 10000))
      service.list(tableId) shouldBe a[Some[_]]
      service.list(tableId).get.length shouldBe n
      (1 to n).par.foreach(x => service.delete(tableId, x))
      service.list(tableId).get.length shouldBe 0
    }
  }
}
