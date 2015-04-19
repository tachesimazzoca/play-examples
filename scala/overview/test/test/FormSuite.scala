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

  case class Item(name: String, variation: Variation, tags: List[String])

  case class Variation(name: String, price: Int)

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

  test("Field") {
    val itemForm = Form(mapping(
      "name" -> nonEmptyText,
      "variation" -> mapping(
        "name" -> nonEmptyText,
        "price" -> number
      )(Variation.apply)(Variation.unapply),
      "tags" -> list(text)
    )(Item.apply)(Item.unapply)).bind(
        Map(
          "name" -> "Play Pants",
          "variation.name" -> "L",
          "variation.price" -> "1350",
          "tags[0]" -> "foo",
          "tags[1]" -> "bar",
          "tags[2]" -> "baz"
        )
      )

    assert("variation_name" === itemForm("variation.name").id)
    assert("variation.name" === itemForm("variation.name").label)
    assert("tags_0" === itemForm("tags[0]").id)
    assert("tags.0" === itemForm("tags[0]").label)
    assert(Seq(0, 1, 2) === itemForm("tags").indexes)
  }
}
