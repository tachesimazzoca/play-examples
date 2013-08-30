package controllers

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._

import org.joda.time.{DateTime, LocalDate}

case class User(
  id: Option[Long],
  name: String,
  email: String,
  password: Option[String],
  gender: String,
  birthdate: Option[LocalDate]
)

object Users extends Controller {
  val userForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "name" -> text.verifying(nonEmpty),
      "email" -> text.verifying(
        pattern(
          // Empty or e-mail format
          """(?:|[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*)""".r,
          "constraint.email",
          "error.email")
      ),
      "password" -> optional(text),
      "gender" -> text.verifying(
        pattern("""(?:0|1|2)""".r)
      ),
      "birthdate" -> optional(jodaLocalDate("yyyy-MM-dd"))
    )(User.apply)(User.unapply)
  )

  def edit(id: Option[Int]) = Action {
    val form = id match {
      case Some(x) => userForm.bind(Map("id" -> x.toString))
      case None => userForm
    }
    Ok(views.html.users.edit(form))
  }

  def submit = Action { implicit request =>
    userForm.bindFromRequest.fold(
      form => BadRequest(views.html.users.edit(form)),
      user => Ok(user.toString)
    )
  }
}
