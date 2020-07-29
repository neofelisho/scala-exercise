package com.github.neofelis.exercise

import scala.collection.concurrent.TrieMap
import scala.reflect.ClassTag

class OrderService[T: ClassTag, K: ClassTag, V: ClassTag] {
  private val store = new TrieMap[T, MapStoreWithTTL[K, V]]()

  def create(tableId: T, key: K, value: V, expiredAt: Long): Unit = {
    store.get(tableId) match {
      case None =>
        val newTableStore = new MapStoreWithTTL[K, V]()
        newTableStore.create(key, value, expiredAt)
        store.put(tableId, newTableStore)
      case Some(tableStore) => tableStore.create(key, value, expiredAt)
    }
  }

  def create(items: Array[(T, K, V, Long)]): Unit = {
    items.groupBy(item => item._1).foreach(group => {
      val tableItems = group._2.map(x => (x._2, x._3, x._4))
      store.get(group._1) match {
        case None =>
          val newTableStore = new MapStoreWithTTL[K, V]()
          newTableStore.create(tableItems)
          store.put(group._1, newTableStore)
        case Some(tableStore) => tableStore.create(tableItems)
      }
    })
  }

  def list(tableId: T): Option[Array[V]] = {
    store.get(tableId) match {
      case None => None
      case Some(tableStore) => tableStore.listValue()
    }
  }

  def get(tableId: T, key: K): Option[V] = {
    store.get(tableId) match {
      case None => None
      case Some(tableStore) => tableStore.get(key)
    }
  }

  def delete(tableId: T, key: K): Option[V] = {
    store.get(tableId) match {
      case None => None
      case Some(tableStore) => tableStore.delete(key)
    }
  }
}
