package com.github.neofelis.exercise.web.actor

import akka.actor.Actor
import com.github.neofelis.exercise.model.MapStoreWithTTL
import com.github.neofelis.exercise.web.message.OrderStore._

class OrderStore(name: String) extends Actor {

  private val store = new MapStoreWithTTL[String, OrderItem]()

  override def receive: Receive = {
    case CreateOrderItem(item) =>
      store.create(item.itemId, item, item.servingAt) match {
        case None => sender() ! OrderCreated
        case Some(_) => sender() ! OrderExists
      }
    //TODO: should return the results individually to make it more clear
    case CreateOrderItems(items) =>
      if (store.create(items.map(item => (item.itemId, item, item.servingAt)).toArray).forall(_.isEmpty)) {
        sender() ! OrderCreated
      } else {
        sender() ! OrderExists
      }
    case ListOrderItems() => {
      val x = store.listValue().toVector
      sender() ! x
    }
    case GetOrderItem(itemId) => sender() ! store.get(itemId)
    case DeleteOrderItem(itemId) => sender() ! store.delete(itemId)
  }

}
