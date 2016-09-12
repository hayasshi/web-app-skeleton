package services.actor

import akka.actor.{ Actor, ActorLogging, Props, SupervisorStrategy }
import dto.Todo
import org.joda.time.DateTime
import shade.memcached.{ Configuration, Memcached }

object TodoServiceProtocol {

  sealed trait Request
  case object GetList extends Request
  case class CreateTodo(text: String, limitAt: DateTime) extends Request
  case class UpdateTodo(id: Long, text: String, limitAt: DateTime) extends Request

  sealed trait Response
  case class Failure(cause: Throwable) extends Response
  case class ListGot(list: Seq[Todo]) extends Response
  case object Success extends Response

}

class TodoServiceActor extends Actor with ActorLogging {
  import TodoServiceProtocol._

  override def supervisorStrategy: SupervisorStrategy = SupervisorStrategy.stoppingStrategy

  val client = Memcached(Configuration(""))(context.dispatcher)

  override def receive: Receive = {
    case msg: Request =>
      val memcached = context.actorOf(TodoMemcachedActor.props(client))
      memcached forward msg
  }

}

object TodoServiceActor {
  def props: Props = Props(classOf[TodoServiceActor])
}
