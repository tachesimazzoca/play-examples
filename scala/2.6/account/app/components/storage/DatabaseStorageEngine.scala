package components.storage

import anorm._
import components.util.Clock
import play.api.db.Database

class DatabaseStorageEngine(
  clock: Clock,
  db: Database,
  table: String
) extends StorageEngine {

  private val encoding = "UTF-8"

  private lazy val SELECT_SQL = SQL(
    s"""
       |SELECT storage_value FROM ${table}
       | WHERE storage_key = {storage_key} LIMIT 1""".stripMargin)

  private lazy val SELECT_FOR_UPDATE_SQL = SQL(
    s"""
       |SELECT storage_value FROM ${table}
       | WHERE storage_key = {storage_key} LIMIT 1
       | FOR UPDATE""".stripMargin)

  private lazy val INSERT_SQL = SQL(
    s"""
       |INSERT INTO ${table} (storage_key, storage_value, modified_at)
       | VALUES ({storage_key}, {storage_value}, {modified_at})""".stripMargin)

  private lazy val UPDATE_SQL = SQL(
    s"""
       |UPDATE ${table} SET storage_value = {storage_value}, modified_at = {modified_at}
       | WHERE storage_key = {storage_key}""".stripMargin)

  private lazy val DELETE_SQL = SQL(
    s"""
       |DELETE FROM ${table}
       | WHERE storage_key = {storage_key}""".stripMargin)

  private lazy val GC_SQL = SQL(
    s"""
       |DELETE FROM ${table}
       | WHERE modified_at < {modified_at}""".stripMargin)

  override def read(key: String): Option[Array[Byte]] =
    db.withConnection { implicit conn =>
      SELECT_SQL.on('storage_key -> key)
        .as(SqlParser.get[String]("storage_value").singleOpt)
        .map(_.getBytes)
    }

  override def write(key: String, data: Array[Byte]): Unit =
    db.withTransaction { implicit conn =>
      val t = clock.currentTimeMillis
      val result = SELECT_FOR_UPDATE_SQL.on('storage_key -> key)
        .as(SqlParser.get[String]("storage_value").singleOpt)
      val params = Seq[NamedParameter](
        'storage_key -> key,
        'storage_value -> new String(data, encoding),
        'modified_at -> new java.util.Date(t)
      )
      if (result.isEmpty) {
        INSERT_SQL.on(params: _*).execute()
      } else {
        UPDATE_SQL.on(params: _*).execute()
      }
    }

  override def delete(key: String): Unit =
    db.withTransaction { implicit conn =>
      DELETE_SQL.on('storage_key -> key).execute()
    }

  override def gc(lifetime: Long): Unit =
    db.withTransaction { implicit conn =>
      val t = clock.currentTimeMillis - (lifetime * 1000L)
      GC_SQL.on('modified_at -> new java.util.Date(t)).execute()
    }
}
