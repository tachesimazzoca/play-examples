package models

import anorm.SqlParser._
import anorm._
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.RandomStringUtils
import play.api.Play.current
import play.api.db._

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

  private val simple = {
    get[Long]("accounts.id") ~
      get[String]("accounts.email") ~
      get[Byte]("accounts.status") map {
      case id ~ email ~ status => Account(
        id,
        email,
        Status.fromValue(status)
      )
    }
  }

  def findById(id: Long): Option[Account] = {
    DB.withConnection { implicit conn =>
      SQL("SELECT * FROM accounts WHERE id = {id}")
        .on('id -> id).as(Account.simple.singleOpt)
    }
  }

  def findByEmail(email: String): Option[Account] = {
    DB.withConnection { implicit conn =>
      SQL("SELECT * FROM accounts WHERE email = {email}")
        .on('email -> email).as(Account.simple.singleOpt)
    }
  }

  def create(account: Account, password: Password): Account = {
    DB.withConnection { implicit conn =>
      SQL( """
            INSERT INTO accounts (
              email, password_salt, password_hash, status
            ) VALUES (
              {email}, {password_salt}, {password_hash}, {status}
            )
           """).on(
          'email -> account.email,
          'password_salt -> password.salt,
          'password_hash -> password.hash,
          'status -> account.status.value
        ).executeInsert[Option[Long]]() match {
        case Some(generatedId) => account.copy(id = generatedId)
        case None => sys.error("Failed to generate accounts.id")
      }
    }
  }

  def create(account: Account, password: String): Account = {
    create(account, hashPassword(password))
  }

  def update(account: Account): Account = {
    DB.withConnection { implicit conn =>
      SQL("UPDATE accounts" +
        " SET email = {email}" +
        " , status = {status}" +
        " WHERE id = {id}")
        .on(
          'email -> account.email,
          'status -> account.status.value,
          'id -> account.id).executeUpdate()
      account
    }
  }

  def updatePassword(id: Long, password: String): Password = {
    val pw = hashPassword(password)
    DB.withConnection { implicit conn =>
      SQL("UPDATE accounts" +
        " SET password_salt = {password_salt}" +
        " , password_hash = {password_hash}" +
        " WHERE id = {id}").on(
          'password_salt -> pw.salt,
          'password_hash -> pw.hash,
          'id -> id).executeUpdate()
    }
    pw
  }

  def activate(id: Long) {
    DB.withConnection { implicit conn =>
      SQL("UPDATE accounts SET status = {status} WHERE id = {id}").on(
        'status -> Status.ACTIVE.value,
        'id -> id
      ).executeUpdate()
    }
  }

  def deactivate(id: Long) {
    DB.withConnection { implicit conn =>
      SQL("UPDATE accounts SET status = {status} WHERE id = {id}").on(
        'status -> Status.INACTIVE.value,
        'id -> id
      ).executeUpdate()
    }
  }

  def hashPassword(password: String,
                   saltOpt: Option[String] = None): Password = {
    val salt = saltOpt.getOrElse(RandomStringUtils.randomAlphabetic(PASSWORD_SALT_LENGTH))
    val hash = DigestUtils.sha1Hex(salt ++ password)
    Password(salt, hash)
  }
}
