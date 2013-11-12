package components.mailer

import settings._

import scala.util.Try
import org.apache.commons.mail.SimpleEmail

trait TextMailer[T] {
  val settings: List[Setting]

  def send(options: List[Setting], param: T): Try[String] = {
    val email = new SimpleEmail()
    (settings ::: options).foreach { setting =>
      setting match {
        case SMTP(host, port) =>
          email.setHostName(host)
          email.setSmtpPort(port)
        case Charset(x) => email.setCharset(x)
        case Subject(x) => email.setSubject(x)
        case From(x) => email.setFrom(x.address, x.personal)
        case To(xs) => xs.foreach(x => email.addTo(x.address, x.personal))
        case Cc(xs) => xs.foreach(x => email.addCc(x.address, x.personal))
        case Bcc(xs) => xs.foreach(x => email.addBcc(x.address, x.personal))
        case Header(k, v) => email.addHeader(k, v)
      }
    }
    email.setMsg(render(param))
    Try(email.send)
  }

  def render(param: T): String
}
