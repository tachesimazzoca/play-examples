package controllers

import play.api.mvc._

object DashboardController extends Controller {

  def index = Action {
    Ok(views.html.dashboard.index())
  }
}
