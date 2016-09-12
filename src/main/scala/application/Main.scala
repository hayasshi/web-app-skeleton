package application

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import dao.rdb.TodoDaoOnRDB
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import net.spy.memcached.{ AddrUtil, ConnectionFactoryBuilder, MemcachedClient }
import routes.ApiRoute
import scalikejdbc.{ ConnectionPool, ConnectionPoolSettings }
import services.TodoService
import shade.memcached.{ Configuration, Memcached }

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {

  val config = ConfigFactory.load()
  val applicationName = config.getString("application.name")

  Class.forName("org.h2.Driver")
  ConnectionPool.singleton(s"jdbc:h2:file:./target/$applicationName;", "sa", "sa", ConnectionPoolSettings(8, 32, 1000, "select 1 as one"))

  implicit val system = ActorSystem(applicationName, config)
  implicit val materializer = ActorMaterializer()
  implicit val executer = system.dispatcher

  // memcached settings
  val memcachedHost = config.getString("application.memcached.host")
  val memcachedPort = config.getInt("application.memcached.port")
  val memcachedClient = Memcached(Configuration(s"$memcachedHost:$memcachedPort"))(system.dispatcher)

  val todoService = new TodoService(new TodoDaoOnRDB, memcachedClient)

  val routes = {
    new ApiRoute(todoService).route
  }

  val host = config.getString("application.host")
  val port = config.getInt("application.port")
  val bindingFuture = Http().bindAndHandle(routes, host, port).foreach(println)

  sys.addShutdownHook({
    Await.result(system.terminate(), 10.seconds)
    ConnectionPool.closeAll()
  })

}
