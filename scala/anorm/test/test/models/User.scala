package test.models

import java.sql.Connection

import anorm._
import play.api.db._

case class User(id: Long, email: String, birthday: java.util.Date, status: Int)

object User {
  def withInMemoryTable(rows: Seq[User])(block: Connection => Any) {
    Databases.withInMemory() { database =>
      implicit val conn = database.getConnection()

      SQL("DROP TABLE IF EXISTS users").executeUpdate()
      SQL( """
             |CREATE TABLE users (
             |  id bigint,
             |  email varchar(255) NOT NULL default '',
             |  birthday datetime,
             |  status tinyint
             |)""".stripMargin).executeUpdate()

      val insertQuery = SQL(
        "INSERT INTO users (id, email, birthday, status)" +
          " VALUES ({id}, {email}, {birthday}, {status})"
      )
      rows.foreach { row =>
        val params = Seq[NamedParameter](
          'id -> row.id,
          'email -> row.email,
          'birthday -> row.birthday,
          'status -> row.status)
        insertQuery.on(params: _*).executeUpdate()
      }

      block(conn)

      SQL("DROP TABLE users").executeUpdate()
    }
  }
}
