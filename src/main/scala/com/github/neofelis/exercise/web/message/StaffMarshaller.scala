package com.github.neofelis.exercise.web.message

import com.github.neofelis.exercise.web.message.OrderStore._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import com.github.neofelis.exercise.web.message.Staff._

trait StaffMarshaller {


  // domain model
  //  final case class RequestItem(menuId: Int)
  //
  //  final case class ResponseItem(id: String, menuId: Int, expectedServingAt: Long)
  //
  //  final case class RequestOrder(tableName: String, items: List[RequestItem])
  //
  //  final case class ResponseOrder(tableName: String, items: List[ResponseItem])

  // formats for unmarshalling and marshalling
  implicit val requestItemFormat: RootJsonFormat[RequestItem] = jsonFormat1(RequestItem)
  implicit val requestOrderFormat: RootJsonFormat[RequestOrder] = jsonFormat2(RequestOrder)
  implicit val orderItemFormat: RootJsonFormat[OrderItem] = jsonFormat3(OrderItem)
  implicit val orderItemsFormat: RootJsonFormat[ItemList] = jsonFormat1(ItemList)
  //  implicit val responseItemFormat: RootJsonFormat[ResponseItem] = jsonFormat3(ResponseItem)

}

