package services.cache

import shade.memcached.Memcached
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

trait MemcachedClient {

  val memcached: Memcached

  def setToMemcached[A](key: String, value: A, expire: Int)(implicit format: JsonFormat[A]): Future[Unit] =
    memcached.set(key, value.toJson.compactPrint, expire.seconds)

  def getFromMemcached[A](key: String)(implicit format: JsonFormat[A], ec: ExecutionContext): Future[Option[A]] =
    memcached.get[String](key).map(_.map(JsString(_).convertTo[A]))

}
