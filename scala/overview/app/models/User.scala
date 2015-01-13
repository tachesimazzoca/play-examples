package models

import org.joda.time.LocalDate

case class User(
  id: Option[Long],
  name: String,
  email: String,
  password: Option[String],
  gender: String,
  birthdate: Option[LocalDate]
)
