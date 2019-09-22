package models

import java.sql.Connection

import anorm._

abstract class AbstractDao[T, U] {

  val tableName: String

  val idColumn: String

  val columns: Seq[String]

  val rowParser: RowParser[T]

  def toNamedParameter(entity: T): Seq[NamedParameter]

  def onUpdate(entity: T): T = identity(entity)

  private lazy val findQuery: SqlQuery =
    SQL(s"""SELECT * FROM ${tableName} WHERE ${idColumn} = {${idColumn}}""")

  private lazy val insertQuery: SqlQuery = SQL(
    s"INSERT INTO ${tableName} (" + columns.mkString(", ") + ") VALUES (" +
      columns.map(x => s"{${x}}").mkString(", ") + ")")

  private lazy val updateQuery: SqlQuery = SQL(
    s"UPDATE ${tableName} SET " + columns.map(x => s"${x} = {${x}}").mkString(", ") +
      s" WHERE ${idColumn} = {${idColumn}}")

  private lazy val deleteQuery: SqlQuery =
    SQL(s"""DELETE FROM ${tableName} WHERE ${idColumn} = {${idColumn}}""")

  private lazy val truncateQuery =
    SQL(s"TRUNCATE TABLE ${tableName}")

  private def toIdValue(id: U): ParameterValue =
    if (id.isInstanceOf[Int]) id.asInstanceOf[Int]
    else if (id.isInstanceOf[Long]) id.asInstanceOf[Long]
    else if (id.isInstanceOf[String]) id.asInstanceOf[String]
    else throw new IllegalArgumentException("The type of id value must be Int/Long/String")

  def find(id: U)(implicit conn: Connection): Option[T] =
    findQuery.on(Symbol(idColumn) -> toIdValue(id)).as(rowParser.singleOpt)(conn)

  def create(entity: T)(implicit conn: Connection): T = {
    insertQuery.on(toNamedParameter(onUpdate(entity)): _*).executeUpdate()(conn)
    entity
  }

  def update(entity: T)(implicit conn: Connection): T = {
    updateQuery.on(toNamedParameter(onUpdate(entity)): _*).executeUpdate()(conn)
    entity
  }

  def delete(id: U)(implicit conn: Connection): Unit = {
    deleteQuery.on(Symbol(idColumn) -> toIdValue(id)).executeUpdate()(conn)
  }

  def truncate()(implicit conn: Connection): Unit = {
    truncateQuery.execute()(conn)
  }
}
