package controllers

import play.api.mvc._

object InjectionController {
  def unapply(a: Any): Boolean = {
    a.isInstanceOf[InjectionController]
  }
}

class InjectionController extends Controller {
  var message: String = ""

  def index = Action {
    Ok(message)
  }
}
