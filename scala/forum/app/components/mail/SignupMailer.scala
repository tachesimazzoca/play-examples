package components.mail

import scala.util.Try

import play.api.Play.current

object SignupMailer {

  implicit val relayHost: RelayHost = MockRelayHost

  private val headers = loadConfiguration("SignupMailer")

  def send(address: String, content: SignupMail): Try[String] = {
    val hs = Charset("iso-2022-jp") :: To(Seq(Address(address))) :: headers
    TextMailer.send(hs, views.txt._mail.signup(content).body)
  }
}

case class SignupMail(sessionKey: String)
