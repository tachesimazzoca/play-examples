package components.mailer

import org.apache.commons.mail.{SimpleEmail, Email}
import play.api.{Configuration, Application}

import scala.collection.JavaConversions._

package object settings {

  abstract class Setting

  case class SMTP(host: String, port: Int) extends Setting

  case class Charset(charset: String) extends Setting

  case class Subject(value: String) extends Setting

  case class From(address: Address) extends Setting

  case class To(recipients: List[Address]) extends Setting

  case class Cc(recipients: List[Address]) extends Setting

  case class Bcc(recipients: List[Address]) extends Setting

  case class Header(name: String, value: String) extends Setting

  case class Address(address: String, personal: String = "")

  def loadSettings(name: String)(implicit app: Application): List[Setting] =
    app.configuration.getConfig("mailer." + name) map { config =>
      val smtp = for (conf <- config.getConfig("SMTP"))
      yield SMTP(
          conf.getString("host").getOrElse("localhost"),
          conf.getInt("port").getOrElse(25)
        )

      val charset = config.getString("Charset").map(Charset)
      val subject = config.getString("Subject").map(Subject)
      val from = for {
        conf <- config.getConfig("From")
        address <- conf.getString("address")
      } yield From(Address(address, conf.getString("personal").getOrElse("")))

      def parseRecipients(name: String): Option[List[Address]] =
        config.getConfigList(name) map { confs =>
          val recipients = for {
            conf <- confs.toList
            address <- conf.getString("address")
          } yield Address(address, conf.getString("personal").getOrElse(""))
          if (!recipients.isEmpty) Some(recipients) else None
        } getOrElse None
      val to = parseRecipients("To").map(To)
      val cc = parseRecipients("Cc").map(Cc)
      val bcc = parseRecipients("Bcc").map(Bcc)

      val headers = for {
        confs <- config.getConfigList("Header").toList
        conf <- confs
        name <- conf.getString("name")
      } yield Header(name, conf.getString("value").getOrElse(""))

      List(smtp, charset, subject, from,
        to, cc, bcc).flatMap(_.toList) ++ headers
    } getOrElse Nil

  def emailFromConfig(config: Configuration): Email = {
    val email = new SimpleEmail()
    // SMTP
    config.getString("SMTP.host").map(email.setHostName)
    config.getInt("SMTP.port").map(email.setSmtpPort)

    config.getString("Charset").map(email.setCharset)
    config.getString("Subject").map(email.setSubject)

    // From
    for {
      from <- config.getConfig("From")
      address <- from.getString("address")
    } {
      email.setFrom(address, from.getString("personal").getOrElse(""))
    }

    // Header
    for {
      confs <- config.getConfigList("Header").toList
      conf <- confs
      name <- conf.getString("name")
    } {
      email.addHeader(name, conf.getString("value").getOrElse(""))
    }

    email
  }
}
