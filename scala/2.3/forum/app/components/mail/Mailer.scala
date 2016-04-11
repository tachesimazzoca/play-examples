package components.mail

import org.apache.commons.mail.SimpleEmail

trait Mailer[T, A] {
  def send(headers: Seq[MailHeader], content: T)(implicit relayHost: RelayHost): A
}

object TextMailer extends Mailer[String, String] {
  def send(headers: Seq[MailHeader], content: String)(implicit relayHost: RelayHost): String = {
    val email = new SimpleEmail()
    headers.foreach {
      case Charset(x) => email.setCharset(x)
      case Subject(x) => email.setSubject(x)
      case From(x) => email.setFrom(x.address, x.personal)
      case To(xs) => xs.foreach(x => email.addTo(x.address, x.personal))
      case Cc(xs) => xs.foreach(x => email.addCc(x.address, x.personal))
      case Bcc(xs) => xs.foreach(x => email.addBcc(x.address, x.personal))
      case Header(k, v) => email.addHeader(k, v)
    }
    email.setMsg(content)

    relayHost match {
      case MockRelayHost =>
        println(headers)
        println(content)
        ""
      case SMTPRelayHost(host, port) =>
        email.setHostName(host)
        email.setSmtpPort(port)
        email.send
    }
  }
}
