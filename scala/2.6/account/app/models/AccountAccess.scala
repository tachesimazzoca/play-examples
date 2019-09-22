package models

case class AccountAccess(
  code: String,
  accountId: Long,
  userAgent: String = "",
  remoteAddress: String = "",
  createdAt: Option[java.util.Date] = None
)

object AccountAccess {
  def generateCode: String = java.util.UUID.randomUUID().toString
}
