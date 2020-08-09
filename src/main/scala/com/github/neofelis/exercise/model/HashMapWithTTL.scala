package com.github.neofelis.exercise.model

import scala.collection.immutable.HashMap
import scala.reflect.ClassTag

class HashMapWithTTL[K: ClassTag, V: ClassTag] {
  private var store = HashMap[K, V]()
  private var expiry = HashMap[Long, Array[K]]()

  def create(key: K, value: V, expiredAt: Long): Option[V] = {
    clearExpired()
    putItemIfAbsent((key, value, expiredAt))
  }

  def get(key: K): Option[V] = {
    clearExpired()
    store
      .view
      .get(key)
  }

  def listValue(): Array[V] = {
    clearExpired()
    store
      .view
      .values
      .toArray
  }

  def listKeys(): Array[K] = {
    clearExpired()
    store
      .view
      .keys
      .toArray
  }

  def delete(key: K): Unit = {
    clearExpired()
    synchronized {
      store = store - key
    }
  }

  private def clearExpired(): Unit = {
    synchronized {
      val expiredItems = expiry
        .view
        .filterKeys(_ < System.currentTimeMillis())

      expiry = expiry -- expiredItems.keys
      store = store -- expiredItems.values.flatten
    }
  }

  def setExpiry(key: K, expiredAt: Long): Unit = {
    synchronized {
      expiry.get(expiredAt) match {
        case None => expiry = expiry + (expiredAt -> Array(key))
        case Some(value) =>
          val newValue = value :+ key
          expiry = expiry + (expiredAt -> newValue)
      }
    }
  }

  private def putItemIfAbsent(item: (K, V, Long)): Option[V] = {
    store.get(item._1) match {
      case Some(value) => Some(value)
      case None =>
        setExpiry(item._1, item._3)
        synchronized {
          store = store + (item._1 -> item._2)
        }
        None
    }
  }
}
