package test

import anorm._
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatestplus.play.OneAppPerSuite
import play.api.db._

@RunWith(classOf[JUnitRunner])
class AnormSuite extends FunSuite with OneAppPerSuite {

  test("NamedParameter") {
    Databases.withInMemory() { database =>

      implicit val conn = database.getConnection()

      SQL("DROP TABLE IF EXISTS users").executeUpdate()
      SQL( """
             |CREATE TABLE users (
             |  id int(11),
             |  email varchar(255) NOT NULL default '',
             |  birthday datetime
             |)""".stripMargin).executeUpdate()

      val timestamp: (Long) => Long = System.currentTimeMillis + _ / 1000 * 1000

      val insertQuery = SQL( """INSERT INTO users VALUES ({id}, {email}, {birthday})""")

      val birthday1 = new java.util.Date(timestamp(0))
      val params1 = Seq[NamedParameter](
        'id -> 1,
        'email -> "user1@example.net",
        'birthday -> birthday1)
      insertQuery.on(params1: _*).executeUpdate()

      val birthday2 = new java.util.Date(timestamp(86400 * 1000))
      val values = Seq[ParameterValue](2, "user2@example.net", birthday2)
      val params2 = Seq[NamedParameter](
        'id -> values(0),
        'email -> values(1),
        'birthday -> values(2))
      insertQuery.on(params2: _*).executeUpdate()

      val selectQuery = SQL( """SELECT * FROM users WHERE id = {id}""")

      case class User(id: Int, email: String, birthday: java.util.Date)

      val parser =
        SqlParser.int("id") ~
          SqlParser.str("email") ~
          SqlParser.date("birthday") map {
          case id ~ email ~ birthday => User(id, email, birthday)
        }

      val row1 = selectQuery.on('id -> 1).as(parser.single)
      assert(row1.id === 1)
      assert(row1.email === "user1@example.net")
      assert(row1.birthday.getTime() === birthday1.getTime())

      val row2 = selectQuery.on('id -> 2).as(parser.single)
      assert(row2.id === 2)
      assert(row2.email === "user2@example.net")
      assert(row2.birthday.getTime() === birthday2.getTime())

      SQL("DROP TABLE users").executeUpdate()
    }
  }
}
