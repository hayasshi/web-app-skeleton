package services.actor

import akka.actor.{ Actor, ActorLogging, Props }
import dao.rdb.TodoDaoOnRDB
import scalikejdbc._

class TodoDatabaseActor extends Actor with ActorLogging {
  import TodoServiceProtocol._

  val todoDao = new TodoDaoOnRDB

  override def receive: Receive = {
    case GetList =>
      sender() ! ListGot(todoDao.findAll)
    case CreateTodo(text, limitAt) =>
      DB.localTx { implicit session =>
        todoDao.create(text, limitAt)
      }
      sender() ! Success
    case UpdateTodo(id, text, limitAt) =>
      DB.localTx { implicit session =>
        todoDao.update(id, text, limitAt)
      }
      sender() ! Success
  }

}

object TodoDatabaseActor {
  def props: Props = Props(classOf[TodoDatabaseActor]).withDispatcher("blocking-io-dispatcher")
}
