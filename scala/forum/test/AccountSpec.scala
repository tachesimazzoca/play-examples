package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

import play.api.db._
import play.api.Play.current

import anorm._

class AccountSpec extends Specification {

  import models._

  "Account model" should {
    "be retrieved by id" in {
      running(FakeApplication()) {
        DB.withConnection { implicit conn =>
          Array(
            "TRUNCATE accounts",
            """
              INSERT INTO accounts (id, email, password_hash, password_salt, active)
              VALUES (1, 'user1@example.net', '', '', 1)
            """
          ).map { SQL(_).execute() }
        }

        Account.findById(1) match {
          case Some(account) =>
            account.id must beSome.which(_ == 1L)
            account.email must equalTo("user1@example.net")
            account.password must beNone
            account.active must equalTo(true)
          case None => failure("The record does not exist")
        }

        Account.findById(2) must beNone
      }
    }

    "be created if needed" in {
      running(FakeApplication()) {
        DB.withConnection { implicit conn =>
          Array(
            "TRUNCATE accounts"
          ).map { SQL(_).execute() }
        }

        Account.create(Account(None, "foo@example.net", Some("bar"), true)) match {
          case Right(account) =>
            account.id must beSome.which(_ == 1L)
            account.email must equalTo("foo@example.net")
            account.password must beNone
            account.active must equalTo(true)
          case Left(msg) => failure(msg)
        }

        Account.create(Account(None, "bar@example.net", Some("baz"), false)) match {
          case Right(account) =>
            account.id must beSome.which(_ == 2L)
            account.email must equalTo("bar@example.net")
            account.password must beNone
            account.active must equalTo(false)
          case Left(msg) => failure(msg)
        }
      }
    }

    "be updated if needed" in {
      running(FakeApplication()) {
        DB.withConnection { implicit conn =>
          Array(
            "TRUNCATE accounts",
            """
              INSERT INTO accounts (id, email, password_hash, password_salt, active)
              VALUES (1, 'user1@example.net', 'pass1', '', 1)
            """
          ).map { SQL(_).execute() }
        }

        Account.update(Account(None, "user2@example.net", Some("pass2"), true)) match {
          case Right(account) => failure("The empty ID account should not be updated.")
          case Left(msg) => msg must equalTo("Account.id is empty.")
        }

        Account.update(Account(Some(2), "user2@example.net", Some("pass2"), true)) match {
          case Right(account) => failure("The non-exist account should not be updated.")
          case Left(msg) => msg must equalTo("The account does not exist.")
        }

        Account.update(Account(Some(1), "user1-2@example.net", Some("pass1-2"), true)) match {
          case Right(account) =>
          case Left(msg) => failure("The account could not be updated.")
        }

        DB.withConnection { implicit conn =>
          val rows = SQL("SELECT * FROM accounts WHERE id = 1").apply()
          rows.length must equalTo(1)
          val row = rows.head
          row[Long]("id") must equalTo(1L)
          row[String]("email") must equalTo("user1-2@example.net")
          row[Boolean]("active") must equalTo(true)
        }
      }
    }

    "be activated if needed" in {
      running(FakeApplication()) {
        DB.withConnection { implicit conn =>
          Array(
            "TRUNCATE accounts",
            """
              INSERT INTO accounts (id, email, password_hash, password_salt, active)
              VALUES (1, 'user1@example.net', 'pass1', '', 0)
            """
          ).map { SQL(_).execute() }
        }

        Account.activate(1)

        DB.withConnection { implicit conn =>
          val rows = SQL("SELECT * FROM accounts WHERE id = 1").apply()
          rows.length must equalTo(1)
          val row = rows.head
          row[Long]("id") must equalTo(1L)
          row[String]("email") must equalTo("user1@example.net")
          row[Boolean]("active") must equalTo(true)
        }
      }
    }

    "be deactivated if needed" in {
      running(FakeApplication()) {
        DB.withConnection { implicit conn =>
          Array(
            "TRUNCATE accounts",
            """
              INSERT INTO accounts (id, email, password_hash, password_salt, active)
              VALUES (1, 'user1@example.net', 'pass1', '', 1)
            """
          ).map { SQL(_).execute() }
        }

        Account.deactivate(1)

        DB.withConnection { implicit conn =>
          val rows = SQL("SELECT * FROM accounts WHERE id = 1").apply()
          rows.length must equalTo(1)
          val row = rows.head
          row[Long]("id") must equalTo(1L)
          row[String]("email") must equalTo("user1@example.net")
          row[Boolean]("active") must equalTo(false)
        }
      }
    }

    "generate md5-hash password with salt" in {
      Account.hashPassword("test", "salt") must equalTo("d653ea7ea31e77b41041e7e3d32e3e4a")
    }
  }
}
