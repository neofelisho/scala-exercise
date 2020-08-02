package com.github.neofelis.exercise.web.actor

import java.util.UUID

import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import com.github.neofelis.exercise.service.OrderService
import com.github.neofelis.exercise.web.message.OrderStore
import com.github.neofelis.exercise.web.message.OrderStore.OrderItem
import com.github.neofelis.exercise.web.message.Staff._

import scala.util.Random

class Staff(timeout: Timeout) extends Actor {

  def createTableStore(name: String): ActorRef = {
    context.actorOf(OrderStore.props(name), name)
  }

  implicit def requestTimeout: Timeout = timeout

  override def receive: Receive = {
    case CreateOrder(order) =>
      val orderItems = order.items.map(mapToOrderItem)

      def createIfTableAbsent(): Unit = {
        val tableStore = createTableStore(order.tableName)
        create(tableStore)
      }

      def create(child: ActorRef): Unit = child.forward(OrderStore.CreateOrderItems(orderItems))

      context.child(order.tableName).fold(createIfTableAbsent())(create)

    case ListItems(tableName) =>
      def notFound(): Unit = sender() ! Iterable.empty[OrderStore.OrderItem]

      def list(child: ActorRef): Unit = child.forward(OrderStore.ListOrderItems())

      context.child(tableName).fold(notFound())(list)


    case GetItem(tableName, itemId) =>
      def notFound(): Unit = sender() ! None

      def get(child: ActorRef): Unit = child.forward(OrderStore.GetOrderItem(itemId))

      context.child(tableName).fold(notFound())(get)

    case DeleteItem(tableName, itemId) =>
      def notFound(): Unit = sender() ! None

      def delete(child: ActorRef): Unit = child.forward(OrderStore.DeleteOrderItem(itemId))

      context.child(tableName).fold(notFound())(delete)
  }

  def mapToOrderItem(request: RequestItem): OrderItem = OrderItem(generateItemId(), request.menuId, calculateServingTime())

  def generateItemId(): String = UUID.randomUUID().toString

  def calculateServingTime(): Long = Random.between(10, 60) * 1000
}
