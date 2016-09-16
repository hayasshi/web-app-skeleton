package application

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import redis.RedisClient
import routes.ApiRoute
import scalikejdbc.{ ConnectionPool, ConnectionPoolSettings }
import services.TodoService
import shade.memcached.{ Configuration, Memcached }

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {

  val config = ConfigFactory.load()
  val applicationName = config.getString("application.name")

  implicit val system = ActorSystem(applicationName, config)
  implicit val materializer = ActorMaterializer()

  // ////
  // ExecutionContext
  implicit val nonBlockingEc = system.dispatcher
  val blockingEc = system.dispatchers.lookup("application.blocking-io-dispatcher")

  // ////
  // DB access settings
  val driverClass = config.getString("application.rdb.driver")
  val dbUrl = config.getString("application.rdb.url")
  val dbUser = config.getString("application.rdb.user")
  val dbPass = config.getString("application.rdb.pass")
  val initialPoolSize = config.getInt("application.rdb.initial-pool-size")
  val maxPoolSize = config.getInt("application.rdb.max-pool-size")
  val connectionTimeout = config.getDuration("application.rdb.connection-timeout").toMillis
  Class.forName(driverClass)
  ConnectionPool.singleton(dbUrl, dbUser, dbPass, ConnectionPoolSettings(initialPoolSize, maxPoolSize, connectionTimeout))

  // ////
  // memcached client settings
  val memcachedHost = config.getString("application.memcached.host")
  val memcachedPort = config.getInt("application.memcached.port")
  val memcachedClient = Memcached(Configuration(s"$memcachedHost:$memcachedPort"))(nonBlockingEc)

  // ////
  // redis client settings
  val redisHost = config.getString("application.redis.host")
  val redisPort = config.getInt("application.redis.port")
  val redisClient = RedisClient(redisHost, redisPort)

  // ////
  // service class settings
  val todoService = new TodoService(memcachedClient, redisClient)(nonBlockingEc, blockingEc)

  // ////
  // route settings
  val routes = {
    new ApiRoute(todoService).route
  }

  // ////
  // http server binding
  val host = config.getString("application.host")
  val port = config.getInt("application.port")
  val bindingFuture = Http().bindAndHandle(routes, host, port).foreach(println)

  sys.addShutdownHook({
    Await.result(system.terminate(), 10.seconds)
    ConnectionPool.closeAll()
  })

}
