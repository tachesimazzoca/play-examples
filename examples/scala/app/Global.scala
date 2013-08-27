import play.api._
import play.api.mvc._

object Global extends GlobalSettings {
  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    (request.method, request.path) match {
      case ("GET", "/routing/items/custom.html") => Some(controllers.Routing.custom("customized", 123))
      case _ => super.onRouteRequest(request)
    }
  }
}
