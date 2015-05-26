package test

import anorm._
import models.{Account, AccountService}
import org.specs2.mutable._
import play.api.db._
import play.api.test._

import scala.util.{Failure, Try}

class AccountServiceSpec extends Specification {

  private val FIXTURE_SQL = Array(
    """
    TRUNCATE TABLE accounts
    """,
    """
    INSERT INTO accounts (id, email, password_salt, password_hash, status)
    VALUES (1, 'user1@example.net', 'slt1', 'hash1', 1)
    """,
    """
    INSERT INTO accounts (id, email, password_salt, password_hash, status)
    VALUES (2, 'user2@example.net', 'slt2', 'hash2', 0)
    """
  )

  "AccountService#findById" should {
    "return Option[Account]" in new WithApplication(FakeApplication()) {
      DB.withConnection { implicit conn =>
        FIXTURE_SQL.map {
          SQL(_).execute()
        }
      }

      val service = new AccountService

      service.findById(1) match {
        case Some(account) =>
          account.id must_== 1L
          account.email must_== "user1@example.net"
          account.status must_== Account.Status.ACTIVE
        case None => failure("The record does not exist")
      }

      service.findById(0) must beNone
      service.findById(-1) must beNone
    }
  }

  "AccountService#create" should {
    "insert a row and then return the new one" in new WithApplication(FakeApplication()) {
      DB.withConnection { implicit conn =>
        SQL("TRUNCATE TABLE accounts").execute()
      }

      val service = new AccountService

      val a1 = service.create(Account(0L, "foo@example.net", Account.Status.ACTIVE), "pass")
      a1.id must_== 1L
      a1.email must_== "foo@example.net"
      a1.status must_== Account.Status.ACTIVE

      val a2 = service.create(Account(0L, "bar@example.net",
        Account.Status.INACTIVE), Account.Password("salt", "hash"))
      a2.id must_== 2L
      a2.email must_== "bar@example.net"
      a2.status must_== Account.Status.INACTIVE

      Try(service.create(Account(0L, "bar@example.net",
        Account.Status.INACTIVE), "pass")) match {
        case Failure(e) => e.isInstanceOf[java.sql.SQLException]
        case _ => failure("accounts.email must has a UNIQUE index")
      }
    }
  }

  "AccountService#update" should {
    "update the row and then return the updated one" in
      new WithApplication(FakeApplication()) {
        DB.withConnection { implicit conn =>
          FIXTURE_SQL.map(SQL(_).execute())

          val service = new AccountService

          val account = service.update(Account(1L, "user1-2@example.net",
            Account.Status.INACTIVE))
          val rows = SQL("SELECT * FROM accounts WHERE id = 1")()
          rows.length must_== 1
          val row1 = rows.head
          row1[Long]("id") must_== account.id
          row1[String]("email") must_== account.email
          row1[Byte]("status") must_== Account.Status.INACTIVE.value

          val row2 = SQL("SELECT * FROM accounts WHERE id = 2")().head
          row2[String]("email") must_== "user2@example.net"
          row2[Byte]("status") must_== Account.Status.INACTIVE.value
        }
      }
  }

  "AccountService#updatePassword" should {
    "update the password of the given ID and then return the Password" in
      new WithApplication(FakeApplication()) {
        DB.withConnection { implicit conn =>
          FIXTURE_SQL.map(SQL(_).execute())

          val service = new AccountService

          val password = service.updatePassword(1L, "deadbeef")
          val row1 = SQL("SELECT * FROM accounts WHERE id = 1")().head
          row1[String]("password_salt") must_== password.salt
          row1[String]("password_hash") must_== password.hash

          val row2 = SQL("SELECT * FROM accounts WHERE id = 2")().head
          row2[String]("password_salt") must_== "slt2"
          row2[String]("password_hash") must_== "hash2"
        }
      }
  }

  "AccountService#activate" should {
    "activate the account if needed" in new WithApplication(FakeApplication()) {
      DB.withConnection { implicit conn =>
        FIXTURE_SQL.map(SQL(_).execute())

        val service = new AccountService

        service.activate(2)
        val row = SQL("SELECT * FROM accounts WHERE id = 2")().head
        row[Byte]("status") must_== Account.Status.ACTIVE.value
      }
    }
  }

  "AccountService#deactivate" should {
    "deactivate the account if needed" in new WithApplication(FakeApplication()) {
      DB.withConnection { implicit conn =>
        FIXTURE_SQL.map(SQL(_).execute())

        val service = new AccountService

        service.deactivate(1)
        val row = SQL("SELECT * FROM accounts WHERE id = 1")().head
        row[Byte]("status") must_== Account.Status.INACTIVE.value
      }
    }
  }
}
