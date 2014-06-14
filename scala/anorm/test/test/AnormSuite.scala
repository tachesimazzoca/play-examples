package test

import org.scalatest.FunSuite

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.scalatestplus.play.OneAppPerSuite

import play.api.test._
import play.api.db._

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

      val timestamp: (Long) => Long = System.currentTimeMillis + _ / 1000 * 1000

      val insertQuery = SQL("""INSERT INTO users VALUES ({id}, {email}, {birthday})""")

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

      val selectQuery = SQL("""SELECT * FROM users WHERE id = {id}""")
      val row1 = selectQuery.on('id -> 1).apply().head
      assert(row1[Int]("id") === 1)
      assert(row1[String]("email") === "user1@example.net")
      assert(row1[java.util.Date]("birthday").getTime() === birthday1.getTime())
      val row2 = selectQuery.on('id -> 2).apply().head
      assert(row2[Int]("id") === 2)
      assert(row2[String]("email") === "user2@example.net")
      assert(row2[java.util.Date]("birthday").getTime() === birthday2.getTime())

      SQL("DROP TABLE users").executeUpdate()
    }
  }
}
