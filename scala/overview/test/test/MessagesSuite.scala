package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.{Lang, Messages}
import play.api.test._

@RunWith(classOf[JUnitRunner])
class MessagesSuite extends FunSuite with OneAppPerSuite {

  implicit override lazy val app: FakeApplication = FakeApplication(
    additionalConfiguration = Map("application.langs" -> "en,de")
  )

  implicit val defaultLang = Lang("en")

  test("messages.default") {
    val key = "errors.required"
    assert("foo must be required" === Messages(key, "foo"))
    Array("en", "fr") foreach { lang =>
      assert("bar must be required" === Messages(key, "bar")(Lang(lang)))
    }
    assert("baz muss erforderlich sein" === Messages(key, "baz")(Lang("de")))
  }
}
