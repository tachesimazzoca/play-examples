package test

import components.storage.{Storage, StorageEngine}
import models.{Signup, SignupService}
import org.specs2.mock.Mockito
import org.specs2.mutable._

class SignupSpec extends Specification with Mockito {

  "SignupSession.create" should {
    "return a new session key" in {
      val engine = mock[StorageEngine]
      val service = new SignupService(new Storage(engine))
      service.create(Signup("user1@example.net"))
      there was one(engine).write(anyString, ===("email=user1%40example.net".getBytes))
    }
  }

  "SignupSession.find" should {
    "return None if the given key doesn't exist" in {
      val engine = mock[StorageEngine]
      engine.read(anyString).returns(None)
      val service = new SignupService(new Storage(engine))
      service.find("key-0123") must beNone
    }

    "return Some(Signup) if the given key exists" in {
      val engine = mock[StorageEngine]
      engine.read(anyString).returns(Some("email=user1%40example.net".getBytes))
      val service = new SignupService(new Storage(engine))
      service.find("key-0123") must beSome(Signup("user1@example.net"))
    }
  }
}
