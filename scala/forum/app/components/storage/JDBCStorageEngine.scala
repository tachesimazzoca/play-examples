package components.storage

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db.DB

class JDBCStorageEngine(table: String) extends StorageEngine {

  private lazy val SELECT_SQL =
    s"""
       |SELECT storage_value FROM ${table}
       | WHERE storage_key = {storage_key} LIMIT 1""".stripMargin

  private lazy val SELECT_FOR_UPDATE_SQL =
    s"""
       |SELECT storage_value FROM ${table}
       | WHERE storage_key = {storage_key} LIMIT 1
       | FOR UPDATE""".stripMargin

  private lazy val INSERT_SQL =
    s"""
       |INSERT INTO ${table} (storage_key, storage_value)
       | VALUES ({storage_key}, {storage_value})""".stripMargin

  private lazy val UPDATE_SQL =
    s"""
       |UPDATE ${table} SET storage_value = {storage_value}
       | WHERE storage_key = {storage_key}""".stripMargin

  private lazy val DELETE_SQL =
    s"""
       |DELETE FROM ${table}
       | WHERE storage_key = {storage_key}""".stripMargin

  override def read(key: String): Option[Array[Byte]] =
    DB.withConnection { implicit conn =>
      SQL(SELECT_SQL).on('storage_key -> key)
        .as(scalar[String].singleOpt)
        .map(_.getBytes)
    }

  override def write(key: String, data: Array[Byte]): Boolean =
    DB.withConnection { implicit conn =>
      val result = SQL(SELECT_FOR_UPDATE_SQL).on('storage_key -> key)()
      if (result.isEmpty) {
        SQL(INSERT_SQL).on(
          'storage_key -> key,
          'storage_value -> new String(data, "UTF-8")
        ).execute()
      } else {
        SQL(UPDATE_SQL).on(
          'storage_key -> key,
          'storage_value -> new String(data, "UTF-8")
        ).execute()
      }
      true
    }

  override def delete(key: String): Boolean =
    DB.withConnection { implicit conn =>
      SQL(DELETE_SQL).on(
        'storage_key -> key
      ).execute()
      true
    }
}
