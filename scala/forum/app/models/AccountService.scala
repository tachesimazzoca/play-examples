package models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._

class AccountService {

  private val simple = {
    get[Long]("accounts.id") ~
      get[String]("accounts.email") ~
      get[String]("accounts.password_salt") ~
      get[String]("accounts.password_hash") ~
      get[Byte]("accounts.status") map {
      case id ~ email ~ passwordSalt ~ passwordHash ~ status =>
        val account = Account(id, email, Account.Status.fromValue(status))
        val password = Account.Password(passwordSalt, passwordHash)
        (account, password)
    }
  }

  def findById(id: Long): Option[Account] = {
    DB.withConnection { implicit conn =>
      SQL("SELECT * FROM accounts WHERE id = {id}")
        .on('id -> id).as(simple.singleOpt).map(_._1)
    }
  }

  def findByEmail(email: String): Option[Account] = {
    DB.withConnection { implicit conn =>
      SQL("SELECT * FROM accounts WHERE email = {email}")
        .on('email -> email).as(simple.singleOpt).map(_._1)
    }
  }

  def create(account: Account, password: Account.Password): Account = {
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
    create(account, Account.hashPassword(password))
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

  def updatePassword(id: Long, password: String): Account.Password = {
    val pw = Account.hashPassword(password)
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
        'status -> Account.Status.ACTIVE.value,
        'id -> id
      ).executeUpdate()
    }
  }

  def deactivate(id: Long) {
    DB.withConnection { implicit conn =>
      SQL("UPDATE accounts SET status = {status} WHERE id = {id}").on(
        'status -> Account.Status.INACTIVE.value,
        'id -> id
      ).executeUpdate()
    }
  }

  def authenticate(email: String, password: String): Option[Account] = {
    DB.withConnection { implicit conn =>
      SQL("SELECT * FROM accounts WHERE email = {email}")
        .on('email -> email).as(simple.singleOpt)
        .flatMap { case (account, pw) =>
        if (Account.hashPassword(password, Some(pw.salt)) == pw) {
          Some(account)
        } else None
      }
    }
  }
}
