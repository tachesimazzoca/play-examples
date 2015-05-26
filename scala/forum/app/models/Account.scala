package models

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.RandomStringUtils

case class Account(id: Long, email: String, status: Account.Status)

object Account {

  case class Password(salt: String, hash: String)

  case class Status(value: Int)

  object Status {
    val INACTIVE = Status(0)
    val ACTIVE = Status(1)

    def fromValue(v: Int): Status = {
      if (v == 0) INACTIVE
      else ACTIVE
    }
  }

  private val PASSWORD_SALT_LENGTH = 4

  def hashPassword(password: String,
                   saltOpt: Option[String] = None): Password = {
    val salt = saltOpt.getOrElse(RandomStringUtils.randomAlphabetic(PASSWORD_SALT_LENGTH))
    val hash = DigestUtils.sha1Hex(salt ++ password)
    Password(salt, hash)
  }
}
