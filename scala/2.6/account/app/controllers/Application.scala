package controllers

import javax.inject.Inject
import play.api.mvc.{AbstractController, ControllerComponents}

class Application @Inject() (
  cc: ControllerComponents,
  pagesController: PagesController
) extends AbstractController(cc) {

  def index() = pagesController.page("index")
}
