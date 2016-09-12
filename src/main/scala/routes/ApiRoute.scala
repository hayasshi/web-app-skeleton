package routes

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.stream.scaladsl.{ Sink, Source }
import dto.Todo
import org.joda.time.DateTime
import services.TodoService
import spray.json.DefaultJsonProtocol

case class TodoListJson(todos: Seq[Todo])
case class WriteTodoJson(text: String, limit_at: DateTime)

class ApiRoute(todoService: TodoService)(implicit system: ActorSystem, materializer: Materializer)
    extends RouteBase
    with SprayJsonSupport
    with DefaultJsonProtocol {
  import routes.json.DateTimeJsonProtocol._
  import routes.json.TodoJsonProtocol._

  implicit val todoListJsonFormat = jsonFormat1(TodoListJson)
  implicit val writeTodoJsonFormat = jsonFormat2(WriteTodoJson)

  val route = pathPrefix("api") {
    path("todo") {
      get {
        val resultFuture = Source.single(NotUsed).via(todoService.listFlow).map(TodoListJson).runWith(Sink.head)
        onSuccess(resultFuture)(complete(_))

        // use actor base logic
//        import akka.pattern._
//        import scala.concurrent.duration._
//        implicit val timeout = 5.seconds
//        val todoServiceActor = system.actorOf(TodoServiceActor.props)
//        val rf = (todoServiceActor ? GetList).mapTo[ListGot]
//        onSuccess(rf)(got => complete(got.list))
      } ~
      post {
        entity(as[WriteTodoJson]) { json =>
          val resultFuture = Source.single((json.text, json.limit_at)).via(todoService.createFlow).runWith(Sink.head)
          onSuccess(resultFuture)(_ => complete(HttpResponse(Accepted)))
        }
      } ~
      path(LongNumber) { id =>
        put {
          entity(as[WriteTodoJson]) { json =>
            val resultFuture = Source.single((id, json.text, json.limit_at)).via(todoService.updateFlow).runWith(Sink.head)
            onSuccess(resultFuture)(_ => complete(HttpResponse(Accepted)))
          }
        }
      }
    }
  }
}
