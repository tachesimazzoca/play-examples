package test

import org.specs2.mutable._

import javax.mail.internet.InternetAddress
import org.jvnet.mock_javamail.Mailbox

import play.api.test._
import play.api.test.Helpers._

import play.api.Play.current

import scala.collection.JavaConversions._
import com.typesafe.config.ConfigFactory
 
class MailerSpec extends Specification {

  import components.mailer._
  import components.mailer.settings._
  import components.form.SignupForm

  case class TestTextMail(args: Seq[Any])

  object TestTextMailer extends TextMailer[TestTextMail] {
    val settings: List[Setting] = List(
      SMTP("localhost", 25),
      Charset("iso-2022-jp"),
      Subject("Test Subject"),
      From(Address("from-user@example.net", "From User"))
    )

    def render(param: TestTextMail) = param.args.toString
  }

  "TextMailer" should {
    "send text email with settings and args" in {
      Mailbox.clearAll

      val recipients = List(
        Address("user1@example.net", "User1"),
        Address("user2@example.net", "User2")
      )
      val args = Seq(1, "message", List("foo", "bar"))
      TestTextMailer.send(List(To(recipients)), TestTextMail(args))

      recipients foreach { recipient =>
        val inbox = Mailbox.get(recipient.address)
        inbox.size() must equalTo(1)
        val from = inbox.get(0).getFrom
        from.length must equalTo(1)
        from(0).toString must equalTo("From User <from-user@example.net>")
        inbox.get(0).getSubject must equalTo("Test Subject")
        inbox.get(0).getContent must equalTo(args.toString)
      }
    }
  }

  "SignupMailer" should {
    "render views.txt._mail.signup" in {
      running(FakeApplication()) {
        val body = SignupMailer.render(
          SignupMail(SignupForm("test@example.net", "password"), "fakeSessionKey"))
        body must equalTo("""Hello play-examples/forum
          |
          |Email: test@example.net
          |Session Key: fakeSessionKey
          |""".stripMargin)
      }
    }
  }

  "loadSettings" should {
    "load settings with Play configuration" in {
      val config = ConfigFactory.parseString("""
        |mailer.TestMailer: {
        |  SMTP: { host: "smtp.example.net", port: 457 }
        |  Header: [
        |    { name: "X-Foo", value: "foo" },
        |    { name: "X-Bar", value: "bar" }
        |  ]
        |  Charset: "iso-2022-jp"
        |  Subject: "Test Subject"
        |  From: { address: "from@example.net" }
        |  To: [
        |    { address: "to1@example.net", personal: "To1" },
        |    { address: "to2@example.net", personal: "To2" }
        |  ]
        |}
      """.stripMargin).root.unwrapped.toMap
      running(FakeApplication(additionalConfiguration = config)) {
        loadSettings("TestMailer") must equalTo(List(
          SMTP("smtp.example.net", 457),
          Charset("iso-2022-jp"),
          Subject("Test Subject"),
          From(Address("from@example.net", "")),
          To(List(Address("to1@example.net", "To1"), Address("to2@example.net", "To2"))),
          Header("X-Foo", "foo"),
          Header("X-Bar", "bar")
        ))
      }
    }

    "load some part of settings and merge defaults" in {
      val config = ConfigFactory.parseString("""
        |mailer.TestMailer: {
        |  SMTP: { host: "smtp.example.net" }
        |  Subject: "Test Subject"
        |  To: [
        |    { address: "to1@example.net", personal: "To1" }
        |  ]
        |}
      """.stripMargin).root.unwrapped.toMap
      running(FakeApplication(additionalConfiguration = config)) {
        loadSettings("TestMailer") must equalTo(List(
          SMTP("smtp.example.net", 25),
          Subject("Test Subject"),
          To(List(Address("to1@example.net", "To1")))
        ))
      }
    }
  }
}
