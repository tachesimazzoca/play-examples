package models

import java.sql.Connection

import javax.inject.{Inject, Singleton}
import anorm._
import components.util.Clock

@Singleton
class AccountDao @Inject() (
  clock: Clock
) extends AbstractDao[Account, Long] {

  val tableName = "account"

  val idColumn = "id"

  val columns = Seq(
    "id", "email", "password_salt", "password_hash",
    "status", "created_at", "updated_at"
  )

  val rowParser: RowParser[Account] = {
    import anorm.SqlParser._
    get[Long]("id") ~
      get[String]("email") ~
      get[String]("password_salt") ~
      get[String]("password_hash") ~
      get[Int]("status") ~
      get[Option[java.util.Date]]("created_at") ~
      get[Option[java.util.Date]]("updated_at") map {
      case id ~ email ~ passwordSalt ~ passwordHash ~
        status ~ createdAt ~ updatedAt =>
        Account(id, email, Account.Password(passwordSalt, passwordHash),
          Account.Status.fromValue(status),
          createdAt.map { ts => new java.util.Date(ts.getTime) },
          updatedAt.map { ts => new java.util.Date(ts.getTime) })
    }
  }

  override def toNamedParameter(account: Account): Seq[NamedParameter] = {
    Seq[NamedParameter](
      'id -> account.id,
      'email -> account.email,
      'password_salt -> account.password.salt,
      'password_hash -> account.password.hash,
      'status -> account.status.value,
      'created_at -> account.createdAt,
      'updated_at -> account.updatedAt
    )
  }

  override def onUpdate(account: Account): Account = {
    val t = clock.currentTimeMillis
    account.copy(
      createdAt = account.createdAt.orElse(Some(new java.util.Date(t))),
      updatedAt = Some(new java.util.Date(t))
    )
  }

  val findByEmailQuery = SQL("SELECT * FROM account WHERE email= {email}")

  def findByEmail(email: String)(implicit conn: Connection): Option[Account] =
    findByEmailQuery.on('email -> email).as(rowParser.singleOpt)(conn)
}
