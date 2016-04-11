package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.data.validation.Constraints._
import play.api.data.validation._

@RunWith(classOf[JUnitRunner])
class ConstraintSuite extends FunSuite {
  test("yesOrNo") {
    val yesOrNo = Constraint[String] { s: String =>
      s match {
        case "yes" | "no" => Valid
        case _ => Invalid("The string must be (yes|no).")
      }
    }

    assert(Valid === yesOrNo("yes"))
    assert(Valid === yesOrNo("no"))
    assert(Valid !== yesOrNo(""))
    assert(Valid !== yesOrNo("yesn"))
  }

  test("range") {
    def range(min: Int, max: Int) = Constraint[Int]("constraint.range", min, max) { v: Int =>
      if (v >= min && v <= max) Valid
      else Invalid("error.range", min, max)
    }

    val validator = range(1, 10)
    assert(Some("constraint.range") === validator.name)
    assert(Seq(1, 10) === validator.args)
    assert(Valid === validator(3))
    validator(0) match {
      case Invalid(errors) =>
        assert(1 === errors.size)
        assert("error.range" === errors(0).message)
        assert(Seq(1, 10) === errors(0).args)
      case _ =>
    }
  }

  test("pattern") {
    val validator = pattern(regex = """^[a-zA-Z0-9]+$""".r, error = "must be alphanumeric")
    assert(Valid === validator("A12345"))
    validator("A-12345") match {
      case Invalid(errors) =>
        assert(1 === errors.size)
        assert("must be alphanumeric" === errors(0).message)
      case _ =>
    }
  }

  test("Invalid ++") {
    val result1 = Invalid(ValidationError("error.foo", 1))
    val result2 = Invalid("error.bar", 2, 3)
    val result3 = result1 ++ result2
    assert(Seq(
      ValidationError("error.foo", 1),
      ValidationError("error.bar", 2, 3)
    ) == result3.errors)
  }
}
