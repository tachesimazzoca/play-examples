package components.mail

trait RelayHost

case object MockRelayHost extends RelayHost

case class SMTPRelayHost(host: String, port: Int) extends RelayHost

