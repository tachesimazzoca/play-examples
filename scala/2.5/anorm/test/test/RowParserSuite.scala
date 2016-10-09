package test

import java.io.{InputStream, Reader}

import anorm._
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import test.models.User

import scala.util.Try

@RunWith(classOf[JUnitRunner])
class RowParserSuite extends FunSuite {

  private def timestamp(x: Long = 0): Long = {
    System.currentTimeMillis + x / 1000 * 1000
  }

  private def toByteSeq(input: InputStream): Seq[Byte] =
    Iterator.continually(input.read).takeWhile(_ != -1).map(_.toByte).toSeq

  private def toCharSeq(reader: Reader): Seq[Char] =
    Iterator.continually(reader.read).takeWhile(_ != -1).map(_.toChar).toSeq

  test("~") {
    val selectQuery = SQL( """SELECT * FROM users WHERE id = {id}""")

    val parser = SqlParser.long("id") ~
      SqlParser.str("email") ~
      SqlParser.date("birthday") ~
      SqlParser.int("status") ~
      SqlParser.binaryStream("icon") ~
      SqlParser.str("description") map {
      case id ~ email ~ birthday ~ state ~ icon ~ desc =>
        User(id, email, new java.util.Date(birthday.getTime),
          state, toByteSeq(icon), desc)
    }

    val user1 = User(1, "user1@example.net",
      new java.util.Date(timestamp()), 1)
    val user2 = User(2, "user2@example.net",
      new java.util.Date(timestamp(86400 * 1000)), 1)
    User.withInMemoryTable(Seq(user1, user2)) { implicit conn =>
      assert(selectQuery.on('id -> 1).as(parser.single) === user1)
      assert(selectQuery.on('id -> 2).as(parser.single) === user2)
    }
  }

  test("~ as case class") {
    val selectQuery = SQL( """SELECT * FROM users WHERE id = {id}""")

    val parser: RowParser[~[~[~[~[~[Long, String], java.util.Date], Int], InputStream], String]] =
      SqlParser.long("id") ~ SqlParser.str("email") ~
        SqlParser.date("birthday") ~
        SqlParser.int("status") ~
        SqlParser.binaryStream("icon") ~
        SqlParser.str("description")
    val userParser: RowParser[User] = parser map {
      case ~(~(~(~(~(id, email), birthday), status), icon), desc) =>
        User(id, email, new java.util.Date(birthday.getTime), status, toByteSeq(icon), desc)
    }

    val user1 = User(1, "user1@example.net",
      new java.util.Date(timestamp()), 1)
    val user2 = User(2, "user2@example.net",
      new java.util.Date(timestamp(86400 * 1000)), 1)
    User.withInMemoryTable(Seq(user1, user2)) { implicit conn =>
      assert(selectQuery.on('id -> 1).as(userParser.single) === user1)
      assert(selectQuery.on('id -> 2).as(userParser.single) === user2)
    }
  }

  test("Row.unapplySeq") {
    val selectQuery = SQL(
      """SELECT id, email, birthday, status, icon, description""" +
        """ FROM users WHERE id = {id}""")

    val parser = RowParser[User] {
      case Row(id: Long, email: String, Some(birthday: java.sql.Timestamp),
      status: Byte, Some(icon: java.sql.Blob), Some(desc: java.sql.Clob)) => {
        Success(User(id, email, new java.util.Date(birthday.getTime),
          status, toByteSeq(icon.getBinaryStream),
          toCharSeq(desc.getCharacterStream).mkString))
      }
      case row => Error(TypeDoesNotMatch(s"unexpected type: $row"))
    }

    val user1 = User(1, "user1@example.net",
      new java.util.Date(timestamp()), 1, Seq(), "Hello, I'm user1")
    val user2 = User(2, "user2@example.net",
      new java.util.Date(timestamp(86400 * 1000)), 1,
      Seq("deadbeef".getBytes: _*), "Nop")

    User.withInMemoryTable(Seq(user1, user2)) { implicit conn =>
      assert(selectQuery.on('id -> 1).as(parser.single) === user1)
      assert(selectQuery.on('id -> 2).as(parser.single) === user2)
    }
  }

  test("Row.unapplySeq #2") {
    val selectQuery = SQL("SELECT id, email FROM users")
    val parser = RowParser[(Long, String)] {
      case Row(id: Long, email: String) if !email.isEmpty =>
        Success(id -> email)
      case row => Error(TypeDoesNotMatch(s"unexpected type: $row"))
    }

    User.withInMemoryTable(
      Seq(
        User(1, "user1@example.net", new java.util.Date(timestamp()), 1),
        User(2, "user2@example.net", new java.util.Date(timestamp()), 1),
        User(3, "", new java.util.Date(timestamp()), 1)
      )
    ) { implicit conn =>
      assert(Try(selectQuery.as(parser.*).toMap).toOption === None)
    }

    User.withInMemoryTable(
      Seq(
        User(1, "user1@example.net", new java.util.Date(timestamp()), 1),
        User(2, "user2@example.net", new java.util.Date(timestamp()), 1),
        User(3, "user3@example.net", new java.util.Date(timestamp()), 1)
      )
    ) { implicit conn =>
      val expected = Map(
        1L -> "user1@example.net",
        2L -> "user2@example.net",
        3L -> "user3@example.net"
      )
      assert(Try(selectQuery.as(parser.*).toMap).toOption === Some(expected))
    }
  }
}
