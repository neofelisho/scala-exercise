package com.github.neofelis.exercise.web.route

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.github.neofelis.exercise.web.message.OrderStore._
import com.github.neofelis.exercise.web.message.Staff._
import com.github.neofelis.exercise.web.message.{Staff, StaffMarshaller}

import scala.concurrent.{ExecutionContextExecutor, Future}

class RestApi(system: ActorSystem, timeout: Timeout) extends StaffRoute {
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def createStaff(): ActorRef = system.actorOf(Staff.props)

  override implicit def requestTimeout: Timeout = timeout
}


trait StaffRoute extends StaffService {

  protected val createOrderRoute: Route = {
    path("order") {
      post {
        entity(as[RequestOrder]) { ro =>
          onSuccess(createOrder(ro)) {
            case OrderCreated => complete(StatusCodes.Created)
            case OrderExists => complete(StatusCodes.BadRequest)
          }
        }
      }
    }
  }

  protected val listItemsRoute: Route = {
    path("order" / Segment) { tableName =>
      get {
        onSuccess(listItems(tableName)) { items =>
          complete(StatusCodes.OK, ItemList(items))
        }
      }
    }
  }

  protected val getItemRoute: Route = {
    pathPrefix("order" / Segment) { tableName =>
      path(Segment) { itemId =>
        get {
          onSuccess(getItem(tableName, itemId)) {
            case Some(item) => complete(StatusCodes.OK, item)
            case None => complete(StatusCodes.NotFound)
          }
        }
      }
    }
  }

  protected val deleteItemRoute: Route = {
    pathPrefix("order" / Segment) { tableName =>
      path(Segment) { itemId =>
        delete {
          onSuccess(deleteItem(tableName, itemId)) {
            case Some(item) => complete(StatusCodes.OK, item)
            case None => complete(StatusCodes.BadRequest)
          }
        }
      }
    }
  }

  val route: Route = createOrderRoute ~ listItemsRoute ~ getItemRoute ~ deleteItemRoute

}


trait StaffService extends StaffMarshaller {

  def createStaff(): ActorRef

  implicit val executionContext: ExecutionContextExecutor

  implicit def requestTimeout: Timeout

  lazy val staff: ActorRef = createStaff()

  def createOrder(requestOrder: RequestOrder): Future[OrderResponse] = {
    staff.ask(CreateOrder(requestOrder)).mapTo[OrderResponse]
  }

  def listItems(tableName: String): Future[Iterable[OrderItem]] = {
    staff.ask(ListItems(tableName)).mapTo[Iterable[OrderItem]]
  }

  def getItem(tableName: String, itemId: String): Future[Option[OrderItem]] = {
    staff.ask(GetItem(tableName, itemId)).mapTo[Option[OrderItem]]
  }

  def deleteItem(tableName: String, itemId: String): Future[Option[OrderItem]] = {
    staff.ask(DeleteItem(tableName, itemId)).mapTo[Option[OrderItem]]
  }

}