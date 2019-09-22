package models

import components.util.SystemClock
import models.test._
import org.scalatest.FunSuite

class AccountAccessDaoSuite extends FunSuite {

  test("add") {
    withTestDatabase() { db =>
      val t = System.currentTimeMillis - 86400000L
      val accountAccessDao = new AccountAccessDao(new SystemClock)

      db.withTransaction { implicit conn =>
        Seq(
          AccountAccess("access-1-1", 1L, "ua", "127.0.0.1", Some(new java.util.Date(t + 1000L))),
          AccountAccess("access-1-2", 1L, "ua", "127.0.0.1", Some(new java.util.Date(t + 2000L))),
          AccountAccess("access-2-1", 2L, "ua", "127.0.0.1", Some(new java.util.Date(t + 3000L))),
          AccountAccess("access-1-3", 1L, "ua", "127.0.0.1", Some(new java.util.Date(t + 4000L))),
          AccountAccess("access-3-1", 3L, "ua", "127.0.0.1", Some(new java.util.Date(t + 5000L))),
          AccountAccess("access-2-2", 2L, "ua", "127.0.0.1", Some(new java.util.Date(t + 6000L)))
        ).foreach(accountAccessDao.add(_, 2))

      }

      db.withConnection { implicit conn =>
        assert(Seq("access-1-3", "access-1-2") ===
          accountAccessDao.selectByAccountId(1L).map(_.code))
        assert(Seq("access-2-2", "access-2-1") ===
          accountAccessDao.selectByAccountId(2L).map(_.code))
        assert(Seq("access-3-1") ===
          accountAccessDao.selectByAccountId(3L).map(_.code))
      }
    }
  }
}
