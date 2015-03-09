package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints._

@RunWith(classOf[JUnitRunner])
class FormSuite extends FunSuite {
  case class User(name: String, age: Int)

  test("bind") {
    val userForm = Form(mapping(
      "name" -> nonEmptyText,
      "age" -> number.verifying(min(0), max(100))
    )(User.apply)(User.unapply))

    val boundForm = userForm.bind(Map("name" -> "Foo", "age" -> "26"))
    assert(Some(User("Foo", 26)) === boundForm.value)
    assert(false === boundForm.hasErrors)

    userForm.bind(Map.empty[String, String]).fold(
      formWithErrors => {
        assert(2 === formWithErrors.errors.size)
      },
      user => {
        fail("It must fail with an empty map.")
      }
    )
  }
}
