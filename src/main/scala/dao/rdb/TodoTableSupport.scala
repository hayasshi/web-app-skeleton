package dao.rdb

import dto.Todo
import scalikejdbc.WrappedResultSet

trait TodoTableSupport {

  def convert(rs: WrappedResultSet): Todo = Todo(
    rs.long("id"),
    rs.string("text"),
    rs.jodaDateTime("limit_at"),
    rs.jodaDateTime("created_at"),
    rs.jodaDateTime("updated_at")
  )

}
