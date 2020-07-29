package com.github.neofelis.exercise

import scala.collection.concurrent.TrieMap
import scala.reflect.ClassTag

/**
 * [[OrderService]] provides a order service used in a restaurant.
 *
 * @tparam T the type of the id of table.
 * @tparam K the type of the key of order item.
 * @tparam V the type of the value of order item.
 */
class OrderService[T: ClassTag, K: ClassTag, V: ClassTag] {
  private val store = new TrieMap[T, MapStoreWithTTL[K, V]]()

  /**
   * Create a new order containing single item.
   *
   * @param tableId the id of table.
   * @param key the key of the order item.
   * @param value the value of the order item.
   * @param expiredAt the time-to-live of the order item.
   */
  def create(tableId: T, key: K, value: V, expiredAt: Long): Unit = {
    store.get(tableId) match {
      case None =>
        val newTableStore = new MapStoreWithTTL[K, V]()
        newTableStore.create(key, value, expiredAt)
        store.put(tableId, newTableStore)
      case Some(tableStore) => tableStore.create(key, value, expiredAt)
    }
  }

  /**
   * Create a new order containing multiple items.
   *
   * @param items an [[Array]] of item containing tableId, key, value and time-to-live.
   */
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

  /**
   * List the order items by tableId.
   *
   * @param tableId the id of table.
   * @return an [[Option]] containing an [[Array]] of order items, or [[None]] if there is no items for the table.
   */
  def list(tableId: T): Option[Array[V]] = {
    store.get(tableId) match {
      case None => None
      case Some(tableStore) => tableStore.listValue()
    }
  }

  /**
   * Get the detail of specific order item.
   *
   * @param tableId the id of table.
   * @param key the key of the order item.
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
   * @param key the key of the order item.
   * @return an [[Option]] containing the deleted item, or [[None]] if nothing has been deleted by the key.
   */
  def delete(tableId: T, key: K): Option[V] = {
    store.get(tableId) match {
      case None => None
      case Some(tableStore) => tableStore.delete(key)
    }
  }
}
