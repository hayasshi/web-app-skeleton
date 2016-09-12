package services.actor

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import dto.Todo
import routes.json.TodoJsonProtocol
import services.cache.MemcachedClient
import shade.memcached.Memcached

import scala.util.{ Success => TrySuccess, Failure => TryFailure }

class TodoMemcachedActor(override val memcached: Memcached) extends Actor with ActorLogging with MemcachedClient {
  import TodoJsonProtocol._
  import TodoServiceProtocol._

  implicit val ec = context.dispatcher

  val database: ActorRef = context.actorOf(TodoDatabaseActor.props)

  override def receive: Receive = initialize

  def initialize: Receive = {
    case GetList =>
      getFromMemcached[Seq[Todo]]("todolist").onComplete {
        case TrySuccess(Some(list)) =>
          sender() ! ListGot(list)
        case TrySuccess(None)       =>
          context.become(waitResponse(sender(), GetList))
          database ! GetList
        case TryFailure(cause)   =>
          log.warning("Fail get from Memcached: {}", cause)
          context.become(waitResponse(sender(), GetList))
          database ! GetList
      }
    case msg: Request =>
      database forward msg
  }

  def waitResponse(replyTo: ActorRef, orgMsg: Request): Receive = {
    case msg: ListGot =>
      setToMemcached("todolist", msg.list, 600).onFailure {
        case cause: Throwable =>
          log.warning("Fail set to Memcached: {}", cause)
      }
      replyTo ! msg
    case msg: Response =>
      replyTo ! msg
  }

}

object TodoMemcachedActor {
  def props(memcached: Memcached): Props = Props(new TodoMemcachedActor(memcached))
}
