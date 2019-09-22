package models

import java.sql.SQLException

import components.util.{Clock, SystemClock}
import models.test._
import org.scalatest.FunSuite

class AccountDaoSuite extends FunSuite {

  private def createClock(t: Long) = new Clock {
    def currentTimeMillis: Long = t
  }

  test("username is unique") {
    withTestDatabase() { db =>
      val accountDao = new AccountDao(new SystemClock)
      val accounts = Seq(
        Account(1L, "alice1@example.net", Account.hashPassword("deadbeef"),
          Account.Status.Active),
        Account(2L, "alice1@example.net", Account.hashPassword("deadbeef"),
          Account.Status.Inactive)
      )
      db.withTransaction { implicit conn =>
        accountDao.create(accounts.head)
      }
      db.withTransaction { implicit conn =>
        intercept[SQLException] {
          accountDao.create(accounts(1))
        }
      }
    }
  }

  test("create / update / find") {
    withTestDatabase() { db =>
      val t = System.currentTimeMillis
      val accountDao = new AccountDao(createClock(t))
      val accounts = Seq(
        Account(1L, "alice@example.net", Account.hashPassword("deadbeef"),
          Account.Status.Active),
        Account(2L, "bob@example.net", Account.hashPassword("deadbeef"),
          Account.Status.Inactive)
      )

      db.withTransaction { implicit conn =>
        accounts.foreach(accountDao.create)
      }

      val inserted = Seq(
        Account(1L, "alice@example.net", accounts.head.password,
          Account.Status.Active,
          Some(new java.util.Date(t)), Some(new java.util.Date(t))),
        Account(2L, "bob@example.net", accounts(1).password,
          Account.Status.Inactive,
          Some(new java.util.Date(t)), Some(new java.util.Date(t)))
      )
      db.withConnection { implicit conn =>
        assert(accountDao.find(0L) === None)
        assert(accountDao.find(1L) === Some(inserted.head))
        assert(accountDao.find(2L) === Some(inserted(1)))
      }

      val updated = Seq(
        Account(1L, "alice1@example.net", accounts.head.password,
          Account.Status.Active,
          Some(new java.util.Date(t)), Some(new java.util.Date(t))),
        Account(2L, "bob@example.net", accounts(1).password,
          Account.Status.Inactive,
          Some(new java.util.Date(t)), Some(new java.util.Date(t)))
      )
      db.withTransaction { implicit conn =>
        accountDao.update(inserted.head.copy(email = "alice1@example.net"))
      }
      db.withConnection { implicit conn =>
        assert(accountDao.find(1L) === Some(updated.head))
        assert(accountDao.find(2L) === Some(updated(1)))
      }
    }
  }
}
