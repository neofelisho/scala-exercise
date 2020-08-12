package com.github.neofelis.exercise.service

import akka.actor.{Actor, ActorRef, Props}
import com.github.neofelis.exercise.service.ActorOrderService._

import scala.collection.immutable.HashMap
import scala.reflect.ClassTag

object ActorOrderService {

  def props[T: ClassTag, K: ClassTag, V: ClassTag](): Props = Props(new ActorOrderService[T, K, V]())

  final case class CreateItem[T, K, V](groupId: T, itemKey: K, item: V, expiredAt: Long, replyTo: ActorRef)

  final case class CreateItems[T, K, V](items: Vector[(T, K, V, Long)], replyTo: ActorRef)

  final case class ListItems[T](groupId: T, replyTo: ActorRef)

  final case class GetItem[T, K](groupId: T, itemKey: K, replyTo: ActorRef)

  final case class DeleteItem[T, K](groupId: T, itemKey: K, replyTo: ActorRef)

  final case object ItemCreated

}

class ActorOrderService[T: ClassTag, K: ClassTag, V: ClassTag]() extends Actor {

  override def receive: Receive = {
    case CreateItem(groupId: T, itemKey: K, item: V, expiredAt: Long, replyTo) =>
      create(groupId, itemKey, item, expiredAt)
      replyTo ! ItemCreated
    case CreateItems(items: Vector[(T, K, V, Long)], replyTo) =>
      bulkCreate(items)
      replyTo ! ItemCreated
    case ListItems(groupId: T, replyTo) => replyTo ! list(groupId)
    case GetItem(groupId: T, itemKey: K, replyTo) => replyTo ! get(groupId, itemKey)
    case DeleteItem(groupId: T, itemKey: K, replyTo) => replyTo ! delete(groupId, itemKey)
  }

  var store = new HashMap[T, HashMap[K, V]]()
  var expiry = new HashMap[Long, Array[(T, K)]]()

  def create(groupId: T, itemKey: K, item: V, expiredAt: Long): Unit = {
    clearExpired()
    setExpiry(groupId, itemKey, expiredAt)
    val tableItems = store.getOrElse(groupId, new HashMap[K, V]()).updated(itemKey, item)
    store = store.updated(groupId, tableItems)
  }

  def bulkCreate(items: Vector[(T, K, V, Long)]): Unit = {
    clearExpired()
    items.groupBy(item => item._1).foreach(group => {
      var tableItems = store.getOrElse(group._1, new HashMap[K, V]())
      group._2.foreach(item => {
        setExpiry(group._1, item._2, item._4)
        tableItems = tableItems.updated(item._2, item._3)
      })
      store = store.updated(group._1, tableItems)
    })
  }

  def list(groupId: T): Option[Vector[V]] = {
    clearExpired()
    store.get(groupId) match {
      case Some(tableItems) =>
        val v = tableItems.values.toVector
        Some(v)
      case None => None
    }
  }

  def get(groupId: T, itemKey: K): Option[V] = {
    clearExpired()
    store.get(groupId) match {
      case Some(value) => value.get(itemKey)
      case None => None
    }
  }

  def delete(groupId: T, itemKey: K): Option[V] = {
    clearExpired()
    store.get(groupId) match {
      case Some(value) => value.get(itemKey) match {
        case Some(item) =>
          val tableItems = value.removed(itemKey)
          store = store.updated(groupId, tableItems)
          Some(item)
        case None => None
      }
      case None => None
    }
  }

  private def setExpiry(groupId: T, itemKey: K, expiredAt: Long): Unit = {
    var keys = expiry.getOrElse(expiredAt, Array.empty[(T, K)])
    keys = keys :+ (groupId, itemKey)
    expiry = expiry + (expiredAt -> keys)
  }

  private def clearExpired(): Unit = {
    val expiredItems = expiry.view.filterKeys(_ < System.currentTimeMillis())
    expiry = expiry -- expiredItems.keys
    expiredItems.values.flatten.foreach(item => delete(item._1, item._2))
  }

}
