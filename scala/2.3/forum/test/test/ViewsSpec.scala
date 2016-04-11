package test

import models.SignUpMailer
import org.specs2.mutable._
import play.api.test._

class ViewsSpec extends Specification {

  "views._helpers.tags.config" should {
    "return the configuration value of the key" in new WithApplication(
      FakeApplication(additionalConfiguration = Map("dead" -> "beef"))) {
      views._helpers.tags.config("nokey", "") must_== ""
      views._helpers.tags.config("nokey", "bar") must_== "bar"
      views._helpers.tags.config("dead", "") must_== "beef"
    }
  }

  "views.txt._mail.sign_up" should {
    "render a mail body" in new WithApplication(
      FakeApplication(
        additionalConfiguration = Map(
          "app.baseurl" -> "http://test.example.net:9000"
        )
      )
    ) {
      val params = SignUpMailer.Params("deadbeef")
      val expected = """Hello play-examples/forum
                       |
                       |http://test.example.net:9000/accounts/activate?code=deadbeef
                       |""".stripMargin

      views.txt._mail.sign_up(params).body must_== expected
    }
  }
}
