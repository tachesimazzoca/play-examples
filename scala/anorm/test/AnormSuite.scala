package test

import org.scalatest.FunSuite
import org.scalatestplus.play.OneAppPerSuite

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import play.api.test._

import play.api.db._
import play.api.Play.current

import anorm._

@RunWith(classOf[JUnitRunner])
class AnormSuite extends FunSuite with OneAppPerSuite {

  implicit override lazy val app = FakeApplication()

  test("NamedParameter") {
    DB.withConnection { implicit conn =>
      SQL("DROP TABLE IF EXISTS users").executeUpdate()
      SQL("""
        |CREATE TABLE users (
        |  id int(11),
        |  email varchar(255) NOT NULL default '',
        |  birthday datetime
        |)""".stripMargin).executeUpdate()

      val expected = new java.util.Date(System.currentTimeMillis / 1000 * 1000)
      //---------------------------
      val params = Seq[NamedParameter](
        'id -> 1,
        'email -> "foo@example.net",
        'birthday -> expected)
      //---------------------------
      //val values = Seq[ParameterValue](1, "foo@example.net", expected)
      //val params = Seq[NamedParameter](
      //  'id -> values(0),
      //  'email-> values(1),
      //  'birthday -> values(2)
      //)
      //---------------------------

      SQL("""INSERT INTO users VALUES ({id}, {email}, {birthday})""")
        .on(params: _*).executeUpdate()

      val row = SQL("""SELECT * FROM users WHERE id = {id}""")
          .on('id -> 1).apply().head
      assert(row[Int]("id") === 1)
      assert(row[String]("email") === "foo@example.net")
      assert(row[java.util.Date]("birthday").getTime() === expected.getTime())
    
      SQL("DROP TABLE users").executeUpdate()
    }
  }
}
