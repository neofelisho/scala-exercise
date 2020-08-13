package com.github.neofelis.exercise.service

import com.github.neofelis.exercise.model.TrieMapWithTTL

import scala.collection.concurrent.TrieMap
import scala.collection.parallel.CollectionConverters._
import scala.reflect.ClassTag

/**
 * [[TrieMapOrderService]] provides a order service used in a restaurant.
 *
 * @tparam T the type of the id of table.
 * @tparam K the type of the key of order item.
 * @tparam V the type of the value of order item.
 */
class TrieMapOrderService[T: ClassTag, K: ClassTag, V: ClassTag] {
  private val store = new TrieMap[T, TrieMapWithTTL[K, V]]()

  /**
   * Create a new order containing single item.
   *
   * @param groupId   the id of table.
   * @param key       the key of the order item.
   * @param value     the value of the order item.
   * @param expiredAt the time-to-live of the order item.
   * @return an [[Option]] containing the value if the key already exist, or [[None]] if the creation succeed.
   */
  def create(groupId: T, key: K, value: V, expiredAt: Long): Option[V] = {
    val tableStore = store.getOrElseUpdate(groupId, new TrieMapWithTTL[K, V]())
    tableStore.create(key, value, expiredAt)
  }

  /**
   * Create a new order containing multiple items.
   *
   * @param items an [[Array]] of item containing tableId, key, value and time-to-live.
   */
  def create(items: Array[(T, K, V, Long)]): Unit = {
    items.groupBy(item => item._1).par.foreach(group => {
      val tableStore = store.getOrElseUpdate(group._1, new TrieMapWithTTL[K, V]())
      tableStore.create(group._2.map(item => (item._2, item._3, item._4)))
    })
  }

  /**
   * List the order items by tableId.
   *
   * @param tableId the id of table.
   * @return an [[Option]] containing an [[Array]] of order items, or [[None]] if there is no items for the table.
   */
  def list(tableId: T): Option[Array[V]] = {
    store.get(tableId) match {
      case None => None
      case Some(tableStore) => Some(tableStore.listValue())
    }
  }

  /**
   * Get the detail of specific order item.
   *
   * @param tableId the id of table.
   * @param key     the key of the order item.
   * @return an [[Option]] containing the order item, or [[None]] if there is no corresponding item.
   */
  def get(tableId: T, key: K): Option[V] = {
    store.get(tableId) match {
      case None => None
      case Some(tableStore) => tableStore.get(key)
    }
  }

  /**
   * Delete an order item.
   *
   * @param tableId the id of the table.
   * @param key     the key of the order item.
   * @return an [[Option]] containing the deleted item, or [[None]] if nothing has been deleted by the key.
   */
  def delete(tableId: T, key: K): Option[V] = {
    store.get(tableId) match {
      case None => None
      case Some(tableStore) => tableStore.delete(key)
    }
  }
}
