package components

import play.api.Application

import scala.collection.JavaConversions._

package object mail {
  def loadConfiguration(name: String)(implicit app: Application): List[MailHeader] =
    app.configuration.getConfig("mailer." + name) map { config =>
      val charset = config.getString("Charset").map(Charset)
      val subject = config.getString("Subject").map(Subject)

      // From:
      val from = for {
        conf <- config.getConfig("From")
        address <- conf.getString("address")
      } yield From(Address(address, conf.getString("personal").getOrElse("")))

      // To: Cc: Bcc:
      def parseRecipients(name: String): Option[List[Address]] =
        config.getConfigList(name) map { cs =>
          val recipients = for {
            conf <- cs.toList
            address <- conf.getString("address")
          } yield Address(address, conf.getString("personal").getOrElse(""))
          if (!recipients.isEmpty) Some(recipients) else None
        } getOrElse None
      val to = parseRecipients("To").map(To)
      val cc = parseRecipients("Cc").map(Cc)
      val bcc = parseRecipients("Bcc").map(Bcc)

      val headers = for {
        cs <- config.getConfigList("Header").toList
        conf <- cs
        name <- conf.getString("name")
      } yield Header(name, conf.getString("value").getOrElse(""))

      // Convert List[Option[MailHeader]] into List[MailHeader]
      List(charset, subject, from,
        to, cc, bcc).flatMap(_.toList) ::: headers

    } getOrElse Nil
}
