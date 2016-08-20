package routes.json

import dto.Todo
import spray.json._

object TodoJsonProtocol extends DefaultJsonProtocol {
  import DateTimeJsonProtocol._

  implicit val todoJsonFormat = jsonFormat5(Todo)

}
