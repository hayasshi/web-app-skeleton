package dao.rdb

import org.joda.time.DateTime
import scalikejdbc._
import dto.Todo

object TodoDaoOnRDB extends SQLSyntaxSupport[Todo] {

  override val tableName = "todo"

  def convert(rs: WrappedResultSet): Todo = Todo(
    rs.long("id"),
    rs.string("text"),
    rs.jodaDateTime("limit_at"),
    rs.jodaDateTime("created_at"),
    rs.jodaDateTime("updated_at")
  )

}

class TodoDaoOnRDB extends {
  import TodoDaoOnRDB._

  def findAll(implicit session: DBSession = AutoSession): Seq[Todo] =
    sql"select * from todo".map(convert).list().apply()

  def findById(id: Long)(implicit session: DBSession = AutoSession): Option[Todo] =
    sql"select * from todo where id = $id".map(convert).headOption().apply()

  def create(text: String, limitAt: DateTime)(implicit session: DBSession): Int =
    sql"insert into todo (text, limit_at, created_at, updated_at) values ($text, $limitAt, current_timestamp, current_timestamp)".update().apply()

  def update(id: Long, text: String, limitAt: DateTime)(implicit session: DBSession): Int =
    sql"update todo set text = $text, limit_at = $limitAt, updated_at = current_timestamp where id = $id".update().apply()

}
