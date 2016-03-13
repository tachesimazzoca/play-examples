package test

import anorm._
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatestplus.play.OneAppPerSuite
import test.models.User

@RunWith(classOf[JUnitRunner])
class RowParserSuite extends FunSuite with OneAppPerSuite {

  private def timestamp(x: Long = 0): Long = {
    System.currentTimeMillis + x / 1000 * 1000
  }

  test("~") {
    var user1 = User(1, "user1@example.net", new java.util.Date(timestamp()), 1)
    var user2 = User(2, "user2@example.net", new java.util.Date(timestamp(86400 * 1000)), 1)

    User.withInMemoryTable(Seq(user1, user2)) { implicit conn =>
      val selectQuery = SQL( """SELECT * FROM users WHERE id = {id}""")

      val parser = SqlParser.int("id") ~
        SqlParser.str("email") ~
        SqlParser.date("birthday") ~
        SqlParser.int("status") map {
        case id ~ email ~ birthday ~ state =>
          User(id, email, new java.util.Date(birthday.getTime), state)
      }

      val row1 = selectQuery.on('id -> 1).as(parser.single)
      assert(row1.id === user1.id)
      assert(row1.email === user1.email)
      assert(row1.birthday === user1.birthday)
      assert(row1.status === user1.status)

      val row2 = selectQuery.on('id -> 2).as(parser.single)
      assert(row2.id === user2.id)
      assert(row2.email === user2.email)
      assert(row2.birthday === user2.birthday)
      assert(row2.status === user2.status)
    }
  }
}
