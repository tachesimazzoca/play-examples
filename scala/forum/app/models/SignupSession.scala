package models

import play.api.Play.current

import play.api.db._
import play.api.libs.json._
import play.api.data.validation.ValidationError

import anorm._
import anorm.SqlParser._

import scala.util.Try
import java.util.UUID

case class SignupSession(
  email: String
)

object SignupSession {
  implicit object SignupSessionReads extends Reads[SignupSession] {
    def reads(json: JsValue) = {
      val opt = json match {
        case obj: JsObject =>
          for {
            email <- obj.value.get("email")
          } yield SignupSession(email.as[String])
        case _ => None
      }
      opt match {
        case Some(x) => JsSuccess(x)
        case None => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.signupsession"))))
      }
    }
  }

  def create(session: SignupSession): String =
    DB.withConnection { implicit conn =>
      val key = java.util.UUID.randomUUID().toString
      SQL("""
        INSERT INTO signup_storage (
          storage_key, storage_value
        ) VALUES (
          {storage_key}, {storage_value}
        )
      """).on(
        'storage_key -> key,
        'storage_value -> serialize(session)
      ).execute()
      key
    }

  def find(key: String): Option[SignupSession] =
    DB.withConnection { implicit conn =>
      SQL("""
        SELECT storage_value FROM signup_storage
          WHERE storage_key = {storage_key}
      """).on('storage_key -> key)
        .as(scalar[String].singleOpt)
        .flatMap(unserialize(_))
    }

  def serialize(session: SignupSession): String =
    Json.stringify(Json.obj("email" -> JsString(session.email)))

  def unserialize(serialized: String): Option[SignupSession] =
    Try(Json.parse(serialized).as[SignupSession]).toOption
}
