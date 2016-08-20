package services

import akka.NotUsed
import akka.stream.scaladsl.Flow
import dao.rdb.TodoDaoOnRDB
import dto.Todo
import org.joda.time.DateTime
import scalikejdbc._

trait TodoService extends ServiceBase {

  val todoDao: TodoDaoOnRDB

  val listFlow: Flow[NotUsed, Seq[Todo], NotUsed] = blockingFlow(Flow[NotUsed].map(_ => todoDao.findAll), "listFlow")

  val createFlow: Flow[(String, DateTime), Boolean, NotUsed] = blockingFlow(Flow[(String, DateTime)].map {
    case (text, limitAt) => DB.localTx { implicit session =>
      val count = todoDao.create(text, limitAt)
      count == 1
    }
  }, "createFlow")

  val updateFlow: Flow[(Long, String, DateTime), Boolean, NotUsed] = blockingFlow(Flow[(Long, String, DateTime)].map {
    case (id, text, limitAt) => DB.localTx { implicit session =>
      val count = todoDao.update(id, text, limitAt)
      count == 1
    }
  }, "updateFlow")

}
