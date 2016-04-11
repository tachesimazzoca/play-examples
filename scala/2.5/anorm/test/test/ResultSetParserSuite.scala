package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import anorm._
import anorm.SqlParser._

import models._

@RunWith(classOf[JUnitRunner])
class ResultSetParserSuite extends FunSuite {
  val parser = long("id") ~ str("email") map {
    case id ~ email => (id -> email)
  }

  test("list") {
    User.withInMemoryTable(Seq()) { implicit conn =>
      assert(SQL("SELECT * FROM users").as(parser.*) == List.empty[(Long, String)])
    }
  }

  test("nonEmptyList") {
    User.withInMemoryTable(Seq()) { implicit conn =>
      intercept[AnormException] {
        SQL("SELECT * FROM users").as(parser.+)
      }
    }
  }

  test("single / singleOpt") {
    val users = Seq(
      User(1L, "user1@example.net", new java.util.Date(), 1),
      User(2L, "user2@example.net", new java.util.Date(), 1)
    )
    val findAll = SQL("SELECT * FROM users")
    val findById = SQL("SELECT * FROM users WHERE id = {id}")

    User.withInMemoryTable(users) { implicit conn =>
      intercept[AnormException] {
        // The result must be exactly one row
        findAll.as(parser.single)
        findAll.as(parser.singleOpt)
      }
    }

    User.withInMemoryTable(users) { implicit conn =>
      intercept[AnormException] {
        findById.on('id -> 0L).as(parser.single)
      }
      assert(findById.on('id -> 0L).as(parser.singleOpt) == None)
    }
  }
}
