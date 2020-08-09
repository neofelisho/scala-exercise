package com.github.neofelis.exercise.model

import java.util.UUID

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpecLike
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class MapStoreWithTTLTest extends AnyPropSpecLike with Matchers with ScalaCheckDrivenPropertyChecks {
  private def initializeMapStore(): HashMapWithTTL[Int, Long] = {
    new HashMapWithTTL[Int, Long]()
//    new MapStoreWithTTL[Int, Long]()
  }

  property("should start with empty store") {
    val store = initializeMapStore()
    store.listValue().length shouldBe 0
  }

  property("can add items in parallel") {
    forAll(Gen.choose(1, 100)) { n =>
      val store = initializeMapStore()
      (1 to n).foreach(x => store.create(x, System.currentTimeMillis(), System.currentTimeMillis() + 10000))
      store.listValue().length shouldBe n
    }
  }

  property("can add bulk items in parallel") {
    forAll(Gen.listOfN(10, Gen.choose(1, 100))) { list =>
      val store = new MapStoreWithTTL[String, Long]()
      list
        .map(n => (1 to n).map(_ => (UUID.randomUUID().toString, System.currentTimeMillis(), System.currentTimeMillis() + 10000)))
        .foreach(l => store.create(l.toArray))
      store.listValue().length shouldBe list.sum
    }
  }

  property("can query the added item") {
    val store = initializeMapStore()
    forAll(Gen.choose(1, 100)) { n =>
      store.create(n, System.currentTimeMillis(), System.currentTimeMillis() + 10000)
      store.get(n) shouldBe a[Some[_]]
    }
  }

  property("won't list expired items") {
    val store = initializeMapStore()
    (1 to 10).foreach(x => store.create(x, System.currentTimeMillis(), System.currentTimeMillis() + 100))
    store.listValue().length shouldBe 10
    Thread.sleep(200)
    store.listValue().length shouldBe 0
  }

  property("can delete items") {
    val store = initializeMapStore()
    forAll(Gen.choose(1, 100)) { n =>
      store.listValue().length shouldBe 0
      (1 to n).foreach(x => store.create(x, System.currentTimeMillis(), System.currentTimeMillis() + 10000))
      store.listKeys() shouldBe a[Array[_]]
      store.listKeys().length shouldBe n
      store.listKeys().foreach(key => store.delete(key))
      store.listValue().length shouldBe 0
    }
  }
}
