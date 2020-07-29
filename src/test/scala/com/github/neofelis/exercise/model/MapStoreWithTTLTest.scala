package com.github.neofelis.exercise.model

import java.util.UUID

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class MapStoreWithTTLTest extends AnyPropSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  property("should start with empty store") {
    val store = new MapStoreWithTTL[Int, Long]()
    store.listValue() shouldBe None
  }

  property("can add items in parallel") {
    forAll(Gen.choose(1, 100)) { n =>
      val store = new MapStoreWithTTL[Int, Long]()
      (1 to n).foreach(x => store.create(x, System.currentTimeMillis(), System.currentTimeMillis() + 10_000))
      store.listValue() match {
        case None =>
        case Some(items) => items.length shouldBe n
      }
    }
  }

  property("can add bulk items in parallel") {
    forAll(Gen.listOfN(10, Gen.choose(1, 100))) { list =>
      val store = new MapStoreWithTTL[String, Long]()
      list
        .map(n => (1 to n).map(_ => (UUID.randomUUID().toString, System.currentTimeMillis(), System.currentTimeMillis() + 10_000)))
        .foreach(l => store.create(l.toArray))
      store.listValue().get.length shouldBe list.sum
    }
  }

  property("can query the added item") {
    val store = new MapStoreWithTTL[Int, Long]()
    forAll(Gen.choose(1, 100)) { n =>
      store.create(n, System.currentTimeMillis(), System.currentTimeMillis() + 10_000)
      store.get(n) shouldBe a[Some[_]]
    }
  }

  property("won't list expired items") {
    val store = new MapStoreWithTTL[Int, Long]()
    (1 to 10).foreach(x => store.create(x, System.currentTimeMillis(), System.currentTimeMillis() + 100))
    store.listValue().get.length shouldBe 10
    Thread.sleep(200)
    store.listValue() shouldBe None
  }

  property("can delete items") {
    val store = new MapStoreWithTTL[Int, Long]()
    forAll(Gen.choose(1, 100)) { n =>
      store.listValue() shouldBe None
      (1 to n).foreach(x => store.create(x, System.currentTimeMillis(), System.currentTimeMillis() + 10_000))
      store.listKeys() shouldBe a[Some[_]]
      store.listKeys().get.length shouldBe n
      store.listKeys().get.foreach(key => store.delete(key))
      store.listValue() shouldBe None
    }
  }
}
