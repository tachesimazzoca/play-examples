package test

import components.storage.{Storage, StorageEngine}
import models.{SignUp, SignUpSession}
import org.specs2.mock.Mockito
import org.specs2.mutable._

class SignUpSpec extends Specification with Mockito {

  "SignUpSession.create" should {
    "return a new session key" in {
      val engine = mock[StorageEngine]
      val session = new SignUpSession(new Storage(engine))
      session.create(SignUp("user1@example.net"))
      there was one(engine).write(anyString, ===("email=user1%40example.net".getBytes))
    }
  }

  "SignUpSession.find" should {
    "return None if the given key doesn't exist" in {
      val engine = mock[StorageEngine]
      engine.read(anyString).returns(None)
      val session = new SignUpSession(new Storage(engine))
      session.find("key-0123") must beNone
    }

    "return Some(SignUp) if the given key exists" in {
      val engine = mock[StorageEngine]
      engine.read(anyString).returns(Some("email=user1%40example.net".getBytes))
      val session = new SignUpSession(new Storage(engine))
      session.find("key-0123") must beSome(SignUp("user1@example.net"))
    }
  }
}
