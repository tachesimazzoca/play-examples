package models

object IdSequence {

  type Assigner = Long => Long

  val increment: Assigner = x => x + 1

  sealed abstract class SequenceType(
    val name: String,
    val assigner: Assigner = increment
  )

  object SequenceType {
    case object Account extends SequenceType("account")
  }
}
