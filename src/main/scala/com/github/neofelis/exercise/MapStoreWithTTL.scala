package com.github.neofelis.exercise

import scala.collection.concurrent.TrieMap
import scala.reflect.ClassTag

class MapStoreWithTTL[K: ClassTag, V: ClassTag]() {

  private val store = TrieMap[K, V]()
  private val expiry = TrieMap[Long, Array[K]]()

  def create(key: K, value: V, expiredAt: Long): Option[V] = {
    clearExpired()
    putItemIfAbsent(key, value, expiredAt)
  }

  def create(list: Array[(K, V, Long)]): Array[Option[V]] = {
    clearExpired()
    list.map(putItemIfAbsent)
  }

  def get(key: K): Option[V] = {
    clearExpired()
    store
      .view
      .get(key)
  }

  def listValue(): Option[Array[V]] = {
    clearExpired()
    val result = store
      .view
      .values
      .toArray
    if (result.length == 0) None else Some(result)
  }

  def listKeys(): Option[Array[K]] = {
    clearExpired()
    val result = store
      .view
      .keys
      .toArray
    if (result.length == 0) None else Some(result)
  }

  def delete(key: K): Option[V] = {
    clearExpired()
    store.remove(key)
  }

  private def setExpiry(key: K, expiredAt: Long): Unit = {
    expiry.get(expiredAt) match {
      case None => expiry.put(expiredAt, Array(key))
      case Some(value) => expiry.put(expiredAt, value.appended(key))
    }
  }

  private def clearExpired(): expiry.type = {
    expiry
      .view
      .filterKeys(_ < System.currentTimeMillis())
      .values
      .flatten
      .foreach(k => {
        store.remove(k)
      })

    expiry
      .filterInPlace((k, _) => k >= System.currentTimeMillis())
  }

  private def putItemIfAbsent(item: (K, V, Long)): Option[V] = {
    store.putIfAbsent(item._1, item._2) match {
      case None =>
        setExpiry(item._1, item._3)
        None
      case Some(value) => Some(value)
    }
  }
}
