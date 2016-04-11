package components.mail

trait MailHeader

case class Address(address: String, personal: String = "")

case class Charset(charset: String) extends MailHeader

case class Subject(value: String) extends MailHeader

case class From(address: Address) extends MailHeader

case class To(recipients: Seq[Address]) extends MailHeader

case class Cc(recipients: Seq[Address]) extends MailHeader

case class Bcc(recipients: Seq[Address]) extends MailHeader

case class Header(name: String, value: String) extends MailHeader

