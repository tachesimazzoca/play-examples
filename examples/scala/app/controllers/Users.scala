package controllers

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._

case class User(name: String, email: String, age: Option[Int])

object Users extends Controller {
  val userForm = Form(
    mapping(
      "name" -> text.verifying(nonEmpty),
      "email" -> of[String].verifying(
        pattern(
          // Empty or e-mail format
          """(?:|[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*)""".r,
          "constraint.email",
          "error.email")
      ),
      "age" -> optional(number.verifying(
        min(10),
        max(100, true)
      ))
    )(User.apply)(User.unapply)
  )

  def edit(id: Option[Int]) = Action {
    Ok(views.html.users.edit(userForm))
  }

  def submit = Action { implicit request =>
    Ok(views.html.users.edit(userForm.bindFromRequest))
  }
}
