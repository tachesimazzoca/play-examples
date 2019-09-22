package components.storage

import components.util.SystemClock
import models.test._
import org.scalatest.FunSuite

class DatabaseStorageEngineSuite extends FunSuite {
  test("write/read/delete") {
    withTestDatabase() { db =>
      val engine = new DatabaseStorageEngine(new SystemClock, db, "session_storage")
      val v = "bar"
      val byteArrayToString: Array[Byte] => String = new String(_, "UTF-8")
      engine.write("foo", v.getBytes)
      assert(Some(v) === engine.read("foo").map(byteArrayToString))
      engine.delete("baz")
      assert(Some(v) === engine.read("foo").map(byteArrayToString))
      engine.delete("foo")
      assert(None === engine.read("foo"))
    }
  }
}
