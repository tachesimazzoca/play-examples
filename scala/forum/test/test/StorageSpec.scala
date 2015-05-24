package test

import components.storage.{Storage, StorageEngine}
import org.specs2.mutable.Specification
import org.specs2.mock._

class StorageSpec extends Specification with Mockito {

  "Storage#create" should {
    "return a new UUID key with the specified namespace" in {
      val engine = mock[StorageEngine]
      val storage = new Storage(engine, "app_")

      val key = storage.create(Map.empty[String, String])
      key must beMatching("app_[-0-9a-f]+".r)
    }

    "write empty bytes if the data is a empty map" in {
      val engine = mock[StorageEngine]
      val storage = new Storage(engine)

      val key = storage.create(Map.empty[String, String])
      there was one(engine).write(anyString, ===("".getBytes))
    }

    "write serialized bytes if the data is not empty" in {
      val engine = mock[StorageEngine]
      val storage = new Storage(engine)
      val key = storage.create(Map("foo" -> "=bar=", "baz" -> "12 34"))

      val expected = "foo=%3Dbar%3D&baz=12+34"
      there was one(engine).write(anyString, ===(expected.getBytes))
    }
  }
}
