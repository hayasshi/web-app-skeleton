package services

import dao.rdb.TodoDaoOnRDB
import dto.Todo
import org.joda.time.DateTime
import scalikejdbc._
import services.cache.MemcachedClient
import shade.memcached.Memcached

import scala.concurrent.{ ExecutionContext, Future }

class TodoService(val todoDao: TodoDaoOnRDB, val memcached: Memcached)(val nonBlockingEc: ExecutionContext, val blockingEc: ExecutionContext)
    extends ServiceBase
    with MemcachedClient {
  import routes.json.TodoJsonProtocol._
  import spray.json._

  def getList: Future[Seq[Todo]] = {
    def getListFromDb(ec: ExecutionContext): Future[Seq[Todo]] = Future(todoDao.findAll)(ec)

    memcached.get[String]("todolist").flatMap {
      case Some(str) =>
        Future.successful(JsString(str).convertTo[Seq[Todo]])
      case None      =>
        val f = getListFromDb(blockingEc)
        f.foreach(list => setToMemcached("todolist", list.toJson.compactPrint, 600))(nonBlockingEc)
        f
    }(nonBlockingEc)
  }

  def create(text: String, limitAt: DateTime): Future[Boolean] = Future {
    DB.localTx { implicit session =>
      val count = todoDao.create(text, limitAt)
      count == 1
    }
  }(blockingEc)

  def update(id: Long, text: String, limitAt: DateTime): Future[Boolean] = Future {
    DB.localTx { implicit session =>
      val count = todoDao.update(id, text, limitAt)
      count == 1
    }
  }(blockingEc)

}
