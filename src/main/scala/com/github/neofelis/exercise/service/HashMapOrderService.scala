package com.github.neofelis.exercise.service

import com.github.neofelis.exercise.model.HashMapWithTTL

import scala.collection.concurrent.TrieMap
import scala.collection.parallel.CollectionConverters._
import scala.reflect.ClassTag

class HashMapOrderService[T: ClassTag, K: ClassTag, V: ClassTag] {
  private val store = new TrieMap[T, HashMapWithTTL[K, V]]()

  def create(tableId: T, key: K, value: V, expiredAt: Long): Option[V] = {
    val tableStore = store.getOrElseUpdate(tableId, new HashMapWithTTL[K, V]())
    tableStore.create(key, value, expiredAt)
  }

  def create(items: Array[(T, K, V, Long)]): Unit =
    items.groupBy(item => item._1).par.foreach(group => {
      val tableStore = store.getOrElseUpdate(group._1, new HashMapWithTTL[K, V]())
      tableStore.create(group._2.map(item => (item._2, item._3, item._4)))
    })

  def list(tableId: T): Option[Array[V]] =
    store.get(tableId) match {
      case Some(tableStore) => Some(tableStore.listValue())
      case None => None
    }

  def get(tableId: T, key: K): Option[V] =
    store.get(tableId) match {
      case Some(tableStore) => tableStore.get(key)
      case None => None
    }

  def delete(tableId: T, key: K): Any =
    store.get(tableId) match {
      case Some(tableStore) => tableStore.delete(key)
      case None => None
    }
}
