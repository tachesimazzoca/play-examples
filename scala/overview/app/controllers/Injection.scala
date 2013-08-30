package controllers

import play.api._
import play.api.mvc._

object Injection {
  def unapply(a: Any): Boolean = {
    a.isInstanceOf[Injection]
  }
}

class Injection extends Controller {
  var message: String = ""

  def index = Action {
    Ok(message)
  }
}
