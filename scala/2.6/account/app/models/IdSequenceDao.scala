package models

import java.sql.Connection

import anorm.SqlParser._
import anorm._
import javax.inject.Singleton

@Singleton
class IdSequenceDao {

  import models.IdSequence._

  private val selectForUpdateQuery =
    SQL(
      """
        |SELECT sequence_value FROM id_sequence
        | WHERE sequence_name = {sequence_name} FOR UPDATE
      """.stripMargin)

  private val updateQuery =
    SQL(
      """
        |UPDATE id_sequence SET sequence_value = {sequence_value}
        | WHERE sequence_name = {sequence_name}
      """.stripMargin)

  def nextId(sequenceType: SequenceType)(implicit conn: Connection): Long = {
    val currentId = selectForUpdateQuery.on(
      'sequence_name -> sequenceType.name
    ).as(get[Long]("sequence_value").single)
    val nextId = sequenceType.assigner(currentId)
    updateQuery.on(
      'sequence_name -> sequenceType.name,
      'sequence_value -> nextId
    ).executeUpdate()
    nextId
  }

  def reset(sequenceType: SequenceType)(implicit conn: Connection): Unit = {
    updateQuery.on(
      'sequence_name -> sequenceType.name,
      'sequence_value -> 0
    ).executeUpdate()
  }
}
