package test

import anorm._
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatestplus.play.OneAppPerSuite
import test.models.User

@RunWith(classOf[JUnitRunner])
class WithResultSuite extends FunSuite with OneAppPerSuite {

  test("as") {
    val parser: RowParser[(Long, String)] =
      SqlParser.long("id") ~ SqlParser.str("email") map {
        case id ~ email => (id -> email)
      }

    val users = Seq(
      User(1L, "user1@example.net", new java.util.Date(), 1),
      User(2L, "user2@example.net", new java.util.Date(), 1),
      User(3L, "user3@example.net", new java.util.Date(), 1)
    )
    User.withInMemoryTable(users) { implicit conn =>
      val userList: List[(Long, String)] =
        SQL("SELECT * FROM users").as(parser.*)
      assert(userList === List(
        1L -> "user1@example.net",
        2L -> "user2@example.net",
        3L -> "user3@example.net"))
    }
  }

  test("withResult") {
    val users = Seq(
      User(1L, "user1@example.net", new java.util.Date(), 1),
      User(2L, "user2@example.net", new java.util.Date(), 0),
      User(3L, "user3@example.net", new java.util.Date(), 1)
    )

    User.withInMemoryTable(users) { implicit conn =>
      @annotation.tailrec
      def go(op: Option[Cursor], acc: List[Row]): List[Row] =
        op match {
          case Some(c) => go(c.next, acc :+ c.row)
          case None => acc
        }

      val result: Either[List[Throwable], List[Row]] =
        SQL("SELECT * FROM users WHERE status = 1 ORDER BY id")
          .withResult(go(_, List.empty[Row]))
      val userList = result.right.get
      assert(userList.size === 2)
      assert(userList(0)[String]("email") === "user1@example.net")
      assert(userList(1)[String]("email") === "user3@example.net")
    }
  }

  test("fold") {
    val users = Seq(
      User(1L, "user1@example.net", new java.util.Date(), 1),
      User(2L, "user2@example.net", new java.util.Date(), 0),
      User(3L, "user3@example.net", new java.util.Date(), 1)
    )

    User.withInMemoryTable(users) { implicit conn =>
      val result: Either[List[Throwable], List[Row]] =
        SQL("SELECT * FROM users ORDER BY id")
          .fold(List.empty[Row]) { (acc, row) =>
          acc :+ row
        }
      val userList = result.right.get
      assert(userList.size === 3)
      assert(userList(0)[String]("email") === "user1@example.net")
      assert(userList(1)[String]("email") === "user2@example.net")
      assert(userList(2)[String]("email") === "user3@example.net")
    }
  }

  test("foldWhile") {
    val users = Seq(
      User(1L, "user1@example.net", new java.util.Date(), 1),
      User(2L, "user2@example.net", new java.util.Date(), 0),
      User(3L, "user3@example.net", new java.util.Date(), 1)
    )

    User.withInMemoryTable(users) { implicit conn =>
      val result: Either[List[Throwable], List[Row]] =
        SQL("SELECT * FROM users ORDER BY id")
          .foldWhile(List.empty[Row]) { (acc, row) =>
          if (acc.size < 2) (acc :+ row, true)
          else (acc, false)
        }
      val userList = result.right.get
      assert(userList.size === 2)
      assert(userList(0)[String]("email") === "user1@example.net")
      assert(userList(1)[String]("email") === "user2@example.net")
    }
  }
}
