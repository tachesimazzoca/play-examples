package test

import com.typesafe.config.ConfigFactory
import org.specs2.mutable._
import play.api.test._

import scala.collection.JavaConversions._

class MailSpec extends Specification {

  import components.mail._

  "loadHeaders" should {
    "load headers with Play configuration" in new WithApplication(
        FakeApplication(additionalConfiguration = ConfigFactory.parseString("""
          |mailer.TestMailer: {
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
        """.stripMargin).root.unwrapped.toMap)) {
      loadConfiguration("TestMailer") must equalTo(List(
        Charset("iso-2022-jp"),
        Subject("Test Subject"),
        From(Address("from@example.net", "")),
        To(List(Address("to1@example.net", "To1"), Address("to2@example.net", "To2"))),
        Header("X-Foo", "foo"),
        Header("X-Bar", "bar")
      ))
    }
  }
}
