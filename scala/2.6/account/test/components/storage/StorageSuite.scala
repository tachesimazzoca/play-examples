package components.storage

import components.util.Chances
import org.scalatest.FunSuite

import scala.collection.mutable.ArrayBuffer

class StorageSuite extends FunSuite {

  class MockStorageEngine extends StorageEngine {

    val gcLog: ArrayBuffer[Long] = new ArrayBuffer[Long]

    override def read(key: String): Option[Array[Byte]] = None

    override def write(key: String, data: Array[Byte]): Unit = {}

    override def delete(key: String): Unit = {}

    override def gc(lifetime: Long): Unit = gcLog.append(lifetime)
  }

  test("Settings") {
    val engine = new MockStorageEngine

    // Never call gc()
    engine.gcLog.clear()
    new Storage(engine, Storage.Settings()).gc()
    assert(engine.gcLog.isEmpty)

    // gc() every time
    engine.gcLog.clear()
    new Storage(engine, Storage.Settings("", Some(1000L), Chances.everyTime)).gc()
    assert(1000L === engine.gcLog(0))
  }
}
