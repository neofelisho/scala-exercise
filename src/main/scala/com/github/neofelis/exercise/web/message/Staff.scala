package com.github.neofelis.exercise.web.message

import akka.actor.Props
import akka.util.Timeout
import com.github.neofelis.exercise.web.actor.Staff

object Staff {

  def props(implicit timeout: Timeout): Props = Props(new Staff(timeout))

  case class RequestItem(menuId: Int)

  case class RequestOrder(tableName: String, items: Iterable[RequestItem])

  case class CreateOrder(order: RequestOrder)

  //  case class ResponseItem(id: String, menuId: Int, servingAt: Long)

  case class ListItems(tableName: String)

  case class GetItem(tableName: String, itemId: String)

  case class DeleteItem(tableName: String, itemId: String)

  //  sealed trait StaffOperation
  //
  //  final case class OrderCreated(order: RequestOrder) extends StaffOperation
  //
  //  final case class OrderExists() extends StaffOperation

}
