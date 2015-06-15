package controllers

import play.api.mvc._

class DashboardController(components: ComponentRegistry) extends Controller {

  private val userAction = new UserAction(components)
  private val memberAction = userAction andThen MemberAction

  def index = memberAction { implicit request =>
    Ok(views.html.dashboard.index())
  }
}
