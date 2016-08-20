package routes

import akka.http.scaladsl.server.Route

trait RouteBase {

  val route: Route

}
