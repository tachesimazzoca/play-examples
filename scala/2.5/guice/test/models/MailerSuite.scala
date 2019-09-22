package models

import modules.AppConfiguration
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

@RunWith(classOf[JUnitRunner])
class MailerSuite extends FunSuite {
  lazy val injector: Injector = new GuiceApplicationBuilder()
    .bindings(new AppConfiguration)
    .injector

  test("create Mailer.Factory") {
    val mailer = injector.instanceOf[Mailer.Factory].create("foo")
    assert("foo-message-id" === mailer.send(Seq("To: user1@example.net"), "Hello"))
  }
}
