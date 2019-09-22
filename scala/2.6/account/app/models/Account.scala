package models

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.RandomStringUtils

case class Account(
  id: Long,
  email: String,
  password: Account.Password,
  status: Account.Status,
  createdAt: Option[java.util.Date] = None,
  updatedAt: Option[java.util.Date] = None
)

object Account {

  sealed abstract class Status(val value: Int)

  object Status {

    case object Inactive extends Status(0)

    case object Active extends Status(1)

    def fromValue(v: Int): Status = v match {
      case 0 => Inactive
      case 1 => Active
    }
  }

  case class Password(salt: String, hash: String) {
    def matches(rawPassword: String): Boolean = {
      hash.equals(DigestUtils.sha1Hex(salt ++ rawPassword))
    }
  }

  private val PASSWORD_SALT_LENGTH = 4

  def hashPassword(
    password: String,
    saltOpt: Option[String] = None
  ): Password = {
    val salt = saltOpt.getOrElse(
      RandomStringUtils.randomAlphabetic(PASSWORD_SALT_LENGTH))
    val hash = DigestUtils.sha1Hex(salt ++ password)
    Password(salt, hash)
  }
}
