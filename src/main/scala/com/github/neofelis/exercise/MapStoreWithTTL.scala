package com.github.neofelis.exercise

import scala.collection.concurrent.TrieMap
import scala.reflect.ClassTag

/**
 * [[MapStoreWithTTL]] is a concurrent thread-safe lock-free [[Map]] with time-to-live mechanism.
 * It provides good random access performance based on [[TrieMap]].
 *
 * @tparam K the type of key.
 * @tparam V the type of value.
 */
class MapStoreWithTTL[K: ClassTag, V: ClassTag]() {

  // store is the main storage for saving key-value pairs
  private val store = TrieMap[K, V]()
  // expiry keeps the time-to-live for each key in the store
  private val expiry = TrieMap[Long, Array[K]]()

  /** Create a key-value pair with TTL.
   *
   * @param   key       is the key of the key-value pair with type [[K]].
   * @param   value     is the value of the key-value pair with type [[V]].
   * @param   expiredAt is the TTL of this key-value pair, the format is UNIX timestamp in milliseconds.
   * @return an [[Option]] containing the value if the key already exist, or [[None]] if the creation succeed.
   */
  def create(key: K, value: V, expiredAt: Long): Option[V] = {
    clearExpired()
    putItemIfAbsent(key, value, expiredAt)
  }

  /** Create multiple key-value pairs with TTL.
   *
   * @param list is an [[Array]] of key-value pairs with their TTL.
   * @return an [[Array]] of [[Option]] containing the value  if the key already exist, or [[None]] if the creation succeed.
   */
  def create(list: Array[(K, V, Long)]): Array[Option[V]] = {
    clearExpired()
    list.map(putItemIfAbsent)
  }

  /** Get value by key.
   *
   * @param key is the key of the key-value pair with type [[K]].
   * @return an [[Option]] containing the original value for comparing, or [[None]] if there is no corresponding value.
   */
  def get(key: K): Option[V] = {
    clearExpired()
    store
      .view
      .get(key)
  }

  /** List all non-expired value.
   *
   * @return an [[Option]] containing an array of value, or [[None]] if there is no live value.
   */
  def listValue(): Option[Array[V]] = {
    clearExpired()
    val result = store
      .view
      .values
      .toArray
    if (result.length == 0) None else Some(result)
  }

  /** List all non-expired keys.
   *
   * @return an [[Option]] containing an array of keys, or [[None]] if there is no live key.
   */
  def listKeys(): Option[Array[K]] = {
    clearExpired()
    val result = store
      .view
      .keys
      .toArray
    if (result.length == 0) None else Some(result)
  }

  /** Delete the key-value pair by given key.
   *
   * @param key is the key of the key-value pair with type [[K]].
   * @return an [[Option]] containing the value if the deletion succeed, or [[None]] if the deletion failed.
   */
  def delete(key: K): Option[V] = {
    clearExpired()
    store.remove(key)
  }

  /** Set the expiry for corresponding key.
   *
   * @param key       is the key of the key-value pair with type [[K]].
   * @param expiredAt is the TTL of this key-value pair, the format is UNIX timestamp in milliseconds.
   */
  private def setExpiry(key: K, expiredAt: Long): Unit = {
    expiry.get(expiredAt) match {
      case None => expiry.put(expiredAt, Array(key))
      case Some(value) => expiry.put(expiredAt, value.appended(key))
    }
  }

  /** Clear the expired key-value pair.
   *
   * @return the expiry map.
   */
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

  /** Put the key-value pair with TTL into the main storage if it's absent.
   *
   * @param item is a 3-tuple of key-value pair with its TTL.
   * @return an [[Option]] with the value if the key already exist. [[None]] if the value have been put into store.
   */
  private def putItemIfAbsent(item: (K, V, Long)): Option[V] = {
    store.putIfAbsent(item._1, item._2) match {
      case None =>
        setExpiry(item._1, item._3)
        None
      case Some(value) => Some(value)
    }
  }
}
