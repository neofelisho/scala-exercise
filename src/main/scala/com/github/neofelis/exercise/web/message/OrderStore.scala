package com.github.neofelis.exercise.web.message

import akka.actor.Props
import akka.actor.typed.ActorRef
import com.github.neofelis.exercise.web.actor.OrderStore

object OrderStore {

  def props(name: String): Props = Props(new OrderStore(name))

  case class OrderItem(itemId: String, menuId: Int, servingAt: Long)

  case class ItemList(items: Iterable[OrderItem])

  case class CreateOrderItem(item: OrderItem)

  case class CreateOrderItems(items: Iterable[OrderItem])

  case class ListOrderItems()

  case class GetOrderItem(itemId: String)

  case class DeleteOrderItem(itemId: String)

  sealed trait OrderResponse

  case object OrderCreated extends OrderResponse

  case object OrderExists extends OrderResponse

}
