package models

import components.mail._
import play.api.Play.current

object SignUpMailer {
  case class Params(sessionKey: String)

  implicit val relayHost: RelayHost = MockRelayHost

  private val headers = loadConfiguration("app.mailer.SignUpMailer")

  def send(address: String, params: Params): String = {
    val hs = Charset("iso-2022-jp") :: To(Seq(Address(address))) :: headers
    TextMailer.send(hs, views.txt._mail.sign_up(params).body)
  }
}
