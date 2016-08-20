package application

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import dao.rdb.TodoDaoOnRDB
import routes.ApiRoute
import scalikejdbc.{ ConnectionPool, ConnectionPoolSettings }
import services.TodoService

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {

  val config = ConfigFactory.load()
  val applicationName = config.getString("application.name")

  Class.forName("org.h2.Driver")
  ConnectionPool.singleton(s"jdbc:h2:mem:$applicationName;", "ra", "ra", ConnectionPoolSettings(8, 32, 1000, "select 1 as one"))

  implicit val system = ActorSystem(applicationName, config)
  implicit val materializer = ActorMaterializer()
  implicit val executer = system.dispatcher

  val host = config.getString("application.host")
  val port = config.getInt("application.port")

  val todoService = new TodoService {
    override val todoDao: TodoDaoOnRDB = new TodoDaoOnRDB
  }

  val routes = {
    new ApiRoute(todoService).route
  }

  val bindingFuture = Http().bindAndHandle(routes, host, port).foreach(println)

  sys.addShutdownHook({
    ConnectionPool.closeAll()
    Await.result(system.terminate(), 10.seconds)
  })

}
