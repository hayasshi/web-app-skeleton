package routes.json

import org.joda.time.DateTime
import spray.json._

object DateTimeJsonProtocol extends DefaultJsonProtocol {

  implicit val dateTimeJsonProtocol = new JsonFormat[DateTime] {

    override def read(json: JsValue): DateTime = json match {
      case JsNumber(value) => new DateTime(value.toLong)
      case _ => deserializationError(s"$json is not a long number.")
    }

    override def write(obj: DateTime): JsValue = JsNumber(obj.getMillis)

  }

}
