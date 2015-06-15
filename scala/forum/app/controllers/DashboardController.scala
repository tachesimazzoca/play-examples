package controllers

import play.api.mvc._

object DashboardController extends Controller {
  def index = UserAction { implicit request =>
    Ok(views.html.dashboard.index())
  }
}
