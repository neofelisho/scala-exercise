package com.github.neofelis.exercise.web.message

object OrderItemMessage {

  // domain model
  final case class RequestItem(tableId: Int, menuId: Int)

  final case class RequestOrder(items: Iterable[RequestItem])

  final case class OrderItem(id: String, tableId: Int, menuId: Int, servingAt: Long)

  final case class ResponseOrder(items: Iterable[OrderItem])

}
