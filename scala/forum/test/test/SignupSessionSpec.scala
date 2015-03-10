package test

import anorm.SqlParser._
import anorm._
import org.specs2.mutable._
import play.api.db._
import play.api.libs.json._
import play.api.test._

class SignupSessionSpec extends Specification {

  import models.SignupSession

  "model.SignupSession" should {
    "implicits Reads[SignupSession]" in {
      JsObject(Seq("email" -> JsString("user@example.net")))
        .as[SignupSession] must equalTo(SignupSession("user@example.net"))

      JsString("non JsObject").as[SignupSession] must throwA[JsResultException]
    }

    "serialize SignupSession to JSON" in {
      SignupSession.serialize(SignupSession("user@example.net")) must
          equalTo("""{"email":"user@example.net"}""")
    }

    "serialize JSON to SignupSession" in {
      SignupSession.unserialize("""{"email":"user@example.net"}""") must
          beSome(SignupSession("user@example.net"))

      SignupSession.unserialize("") must beNone
      SignupSession.unserialize("[invalid json]") must beNone
      SignupSession.unserialize("{}") must beNone
      SignupSession.unserialize("""{"unknown":"key"}""") must beNone
    }

    "create a new session with UUID key" in new WithApplication(FakeApplication()) {
      DB.withConnection { implicit conn =>
        Array(
          "TRUNCATE TABLE signup_storage"
        ).map { SQL(_).execute() }

        val key = SignupSession.create(SignupSession("user@example.net"))
        key must beMatching("^[0-9a-f]+(-[0-9a-f]+){4}$")

        SQL("SELECT COUNT(*) as c FROM signup_storage").as(scalar[Long].single) must equalTo(1L)

        SQL("""
          SELECT storage_value FROM signup_storage WHERE storage_key = {storage_key}
        """).on('storage_key -> key)
          .as(scalar[String].single) must equalTo("""{"email":"user@example.net"}""")
      }
    }

    "find a existing session by UUID key" in new WithApplication(FakeApplication()) {
      DB.withConnection { implicit conn =>
        Array(
          "TRUNCATE TABLE signup_storage"
        ).map { SQL(_).execute() }

        val key = java.util.UUID.randomUUID().toString
        SignupSession.find(key) must beNone

        val email = "user@example.net"
        SQL("""
          INSERT INTO signup_storage (storage_key, storage_value)
          VALUES ({storage_key}, {storage_value})
        """).on(
          'storage_key -> key,
          'storage_value -> """{"email":"%s"}""".format(email)
        ).execute()

        SignupSession.find(key) must beSome(SignupSession(email))
      }
    }
  }
}
