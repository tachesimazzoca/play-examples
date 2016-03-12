package test

import java.sql.Connection

import anorm._
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatestplus.play.OneAppPerSuite
import play.api.db._

@RunWith(classOf[JUnitRunner])
class AnormSuite extends FunSuite with OneAppPerSuite {

  case class User(id: Int, email: String, birthday: java.util.Date)

  private def withUserTable(rows: Seq[User])(block: Connection => Any) {
    Databases.withInMemory() { database =>
      implicit val conn = database.getConnection()

      SQL("DROP TABLE IF EXISTS users").executeUpdate()
      SQL( """
             |CREATE TABLE users (
             |  id int(11),
             |  email varchar(255) NOT NULL default '',
             |  birthday datetime
             |)""".stripMargin).executeUpdate()

      val insertQuery = SQL(
        """INSERT INTO users VALUES ({id}, {email}, {birthday})"""
      )
      rows.foreach { row =>
        val params = Seq[NamedParameter](
          'id -> row.id,
          'email -> row.email,
          'birthday -> row.birthday)
        insertQuery.on(params: _*).executeUpdate()
      }

      block(conn)

      SQL("DROP TABLE users").executeUpdate()
    }
  }

  private def timestamp(x: Long = 0): Long = {
    System.currentTimeMillis + x / 1000 * 1000
  }

  test("SqlParser") {
    val rows = Seq(
      User(1, "user1@example.net", new java.util.Date(timestamp())),
      User(2, "user2@example.net", new java.util.Date(timestamp(86400 * 1000)))
    )

    withUserTable(rows) { implicit conn =>
      val selectQuery = SQL( """SELECT * FROM users WHERE id = {id}""")

      val parser = SqlParser.int("id") ~
        SqlParser.str("email") ~
        SqlParser.date("birthday") map {
        case id ~ email ~ birthday => User(id, email, new java.util.Date(birthday.getTime))
      }

      val row1 = selectQuery.on('id -> 1).as(parser.single)
      assert(row1.id === rows(0).id)
      assert(row1.email === rows(0).email)
      assert(row1.birthday === rows(0).birthday)

      val row2 = selectQuery.on('id -> 2).as(parser.single)
      assert(row2.id === rows(1).id)
      assert(row2.email === rows(1).email)
      assert(row2.birthday === rows(1).birthday)
    }
  }
}
