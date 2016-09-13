package services

import dao.rdb.TodoTableSupport
import dto.Todo
import org.joda.time.DateTime
import redis.RedisClient
import scalikejdbc._
import shade.memcached.Memcached

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

class TodoService(val memcached: Memcached, val redis: RedisClient)(val nonBlockingEc: ExecutionContext, val blockingEc: ExecutionContext)
    extends ServiceBase
    with TodoTableSupport {
  import routes.json.TodoJsonProtocol._
  import spray.json._

  def getList: Future[Seq[Todo]] = {
    def getListFromDb(ec: ExecutionContext): Future[Seq[Todo]] = Future {
      DB.autoCommit { implicit session =>
        sql"select * from todo".map(convert).list().apply()
      }
    }(ec)

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
      val count = sql"insert into todo (text, limit_at, created_at, updated_at) values ($text, $limitAt, current_timestamp, current_timestamp)".update().apply()
      count == 1
    }

  }(blockingEc)

  def update(id: Long, text: String, limitAt: DateTime): Future[Boolean] = Future {
    DB.localTx { implicit session =>
      val count = sql"update todo set text = $text, limit_at = $limitAt, updated_at = current_timestamp where id = $id".update().apply()
      count == 1
    }
  }(blockingEc)

}
