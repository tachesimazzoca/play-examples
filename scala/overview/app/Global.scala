import play.api._
import play.api.mvc._

object Global extends GlobalSettings {
  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    (request.method, request.path) match {
      case ("GET", "/routing/items/custom.html") => Some(controllers.Routing.custom("customized", 123))
      case _ => super.onRouteRequest(request)
    }
  }

  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    val obj = controllerClass.newInstance
    obj match {
      case controllers.Injection() =>
        obj.asInstanceOf[controllers.Injection].message = "An injection message"
      case _ =>
    }
    obj
  }
}
