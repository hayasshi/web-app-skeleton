package services

import dao.rdb.TodoDaoOnRDB
import dto.Todo
import org.joda.time.DateTime
import redis.RedisClient
import scalikejdbc._
import shade.memcached.Memcached

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

class TodoService(val todoDao: TodoDaoOnRDB, val memcached: Memcached, val redis: RedisClient)(val nonBlockingEc: ExecutionContext, val blockingEc: ExecutionContext)
    extends ServiceBase {
  import routes.json.TodoJsonProtocol._
  import spray.json._

  def getList: Future[Seq[Todo]] = {
    def getListFromDb(ec: ExecutionContext): Future[Seq[Todo]] = Future(todoDao.findAll)(ec)

    memcached.get[String]("todolist").flatMap {
      case Some(str) =>
        Future.successful(JsString(str).convertTo[Seq[Todo]])
      case None      =>
        val f = getListFromDb(blockingEc)
        f.foreach(list => memcached.set("todolist", list.toJson.compactPrint, 600.seconds))(nonBlockingEc)
        f.foreach(list => redis.set("redis::todolist", list.toJson.compactPrint))(nonBlockingEc)
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
