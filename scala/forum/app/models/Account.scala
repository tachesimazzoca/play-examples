package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

import java.security.MessageDigest

case class Account(
  id: Option[Long],
  email: String,
  password: Option[String],
  active: Boolean
)

object Account {
  val simple = {
    get[Option[Long]]("accounts.id") ~
    get[String]("accounts.email") ~
    get[Boolean]("accounts.active") map {
      case id ~ email ~ active => Account(id, email, None, active)
    }
  }

  def findById(id: Long): Option[Account] = {
    DB.withConnection { implicit conn =>
      SQL("SELECT * FROM accounts WHERE id = {id}")
        .on('id -> id).as(Account.simple.singleOpt)
    }
  }

  def create(account: Account): Either[String, Account] = {
    account.id match {
      case Some(id) => Left("Account.id is not empty.")
      case None =>
        DB.withConnection { implicit conn =>
          val salt = ""
          SQL("""
            INSERT INTO accounts (
              email, password_hash, password_salt, active
            ) VALUES (
              {email}, {password_hash}, {password_salt}, {active}
            )
          """).on(
            'email -> account.email,
            'password_hash -> Account.hashPassword(
                account.password.getOrElse(""), salt),
            'password_salt -> salt,
            'active -> (if (account.active) 1 else 0)
          ).executeInsert() match {
            case Some(id) => Right(Account(Some(id), account.email, None, account.active))
            case None     => Left("accounts.id is not generated.")
          }
        }
    }
  }

  def update(account: Account): Either[String, Account] = {
    account.id match {
      case Some(id) =>
        DB.withConnection { implicit conn =>
          val inSql = "UPDATE accounts SET email = {email}" + {
            account.password match {
              case Some(password) =>
                """
                , password_hash = {password_hash}
                , password_salt = {password_salt}
                """
              case None => ""
            }
          } + " WHERE id = {id}"

          val params = {
            Seq(
              'id -> id,
              'email -> account.email
            ) ++ {
              account.password match {
                case Some(password) =>
                  val salt = ""
                  Seq(
                    'password_hash -> Account.hashPassword(
                        account.password.getOrElse(""), salt),
                    'password_salt -> salt
                  )
                case None => Seq()
              }
            }
          }.map(v => v._1 -> toParameterValue(v._2))

          SQL(inSql).on(params: _*).executeUpdate() match {
            case 0 => Left("The account does not exist.")
            case _ => Right(account)
          }
        }
      case None => Left("Account.id is empty.")
    }
  }

  def activate(id: Long) {
    DB.withConnection { implicit conn =>
      SQL("UPDATE accounts SET active = 1 WHERE id = {id}").on(
        'id -> id
      ).executeUpdate()
    }
  }

  def deactivate(id: Long) {
    DB.withConnection { implicit conn =>
      SQL("UPDATE accounts SET active = 0 WHERE id = {id}").on(
        'id -> id
      ).executeUpdate()
    }
  }

  def hashPassword(password: String, salt: String): String = {
    MessageDigest.getInstance("MD5").digest((salt + password).getBytes)
      .map("%02x".format(_)).mkString
  }
}
