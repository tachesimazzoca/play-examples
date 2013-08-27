package controllers

import play.api._
import play.api.mvc._

object Routing extends Controller {
  def query(
    n: Int,
    s: String,
    l: List[String],
    o: Option[String]
  ) = Action {
    Ok("%d %s %s %s".format(n, s, l, o))
  }

  def show(id: Long) = Action {
    Ok(":id = %d".format(id))
  }

  def list(code: String, page: Int) = Action {
    Ok(":code = %s, page = %d".format(code, page))
  }

  /** The route setting of this action does not exist at "conf/routes".
    * It is invoked by an interceptor Global.onRouteRequest.
    *
    * @see [[Global.onRouteRequest]]
    */
  def custom(code: String, page: Int) = Action {
    Ok(":code = %s, page = %d".format(code, page))
  }
}
