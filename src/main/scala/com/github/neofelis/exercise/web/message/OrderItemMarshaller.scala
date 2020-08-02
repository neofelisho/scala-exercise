package com.github.neofelis.exercise.web.message

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import com.github.neofelis.exercise.web.message.OrderItemMessage._

trait OrderItemMarshaller {

  // formats for unmarshalling and marshalling
  implicit val requestItemFormat: RootJsonFormat[RequestItem] = jsonFormat2(RequestItem)
  implicit val responseItemFormat: RootJsonFormat[OrderItem] = jsonFormat4(OrderItem)
  implicit val requestOrderFormat: RootJsonFormat[RequestOrder] = jsonFormat1(RequestOrder)
  implicit val responseOrderFormat: RootJsonFormat[ResponseOrder] = jsonFormat1(ResponseOrder)

}
