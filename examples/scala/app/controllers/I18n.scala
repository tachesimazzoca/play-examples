package controllers

import play.api._
import play.api.mvc._

object I18n extends Controller {
  def messages = Action {
    Ok(views.html.i18n.messages(
      play.api.i18n.Messages.messages(Play.current)))
  }
}
