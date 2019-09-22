package controllers

import javax.inject.Inject
import play.api.mvc._
import play.twirl.api.{HtmlFormat, Template0}

import scala.util.Try

class PagesController @Inject() (
  cc: ControllerComponents
) extends AbstractController(cc) {

  private def companion[T](name: String)(implicit man: Manifest[T]): T =
    Class.forName(name + "$").getField("MODULE$").get(man.runtimeClass).asInstanceOf[T]

  def parsePathString(name: String): Option[String] = {
    val basename = name.replaceAll(".html$", "")
    if (!basename.matches("([_0-9a-zA-Z]+/)*[_0-9a-zA-Z]+/?")) None
    else {
      val path = {
        if (basename.endsWith("/")) basename ++ "index"
        else basename
      }
      Some("views.html.pages." ++ path.replace("/", "."))
    }
  }

  def index() = Action {
    Redirect(routes.Application.index())
  }

  def page(name: String) = Action {
    (for {
      classpath <- parsePathString(name)
      view <- Try(companion[Template0[HtmlFormat.Appendable]](classpath)).toOption
    } yield view.render()).map { html =>
      Ok(html)
    }.getOrElse {
      NotFound
    }
  }
}
