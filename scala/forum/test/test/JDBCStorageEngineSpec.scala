package test

import anorm._
import components.storage.JDBCStorageEngine
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable._
import play.api.db._
import play.api.test._

class JDBCStorageEngineSpec extends Specification {

  val storage = new JDBCStorageEngine("test_storage")

  private abstract class WithTables extends WithApplication(FakeApplication()) {
    override def around[T: AsResult](t: => T): Result = super.around {
      before()
      val r = t
      after()
      r
    }

    def before() {
      DB.withConnection { implicit conn =>
        SQL("DROP TABLE IF EXISTS test_storage").execute()
        SQL(
          """
            |CREATE TABLE test_storage (
            |  storage_key varchar(255) NOT NULL,
            |  storage_value text NULL,
            |  storage_timestamp timestamp,
            |  PRIMARY KEY (storage_key)
            |)
          """.stripMargin).execute()
      }
    }

    def after() {
      DB.withConnection { implicit conn =>
        SQL("DROP TABLE IF EXISTS test_storage").execute()
      }
    }
  }

  sequential

  "JDBCStorageEngine" should {
    "writes, reads and deletes data for a key" in new WithTables {
      storage.write("foo", "bar".getBytes) must beTrue
      storage.read("foo") must beSome("bar".getBytes)
      storage.read("bar") must beNone

      storage.write("bar", "bar".getBytes) must beTrue
      storage.read("foo") must beSome("bar".getBytes)
      storage.read("bar") must beSome("bar".getBytes)

      storage.write("bar", "baz".getBytes) must beTrue
      storage.read("foo") must beSome("bar".getBytes)
      storage.read("bar") must beSome("baz".getBytes)

      storage.delete("foo") must beTrue
      storage.read("foo") must beNone
      storage.read("bar") must beSome("baz".getBytes)

      storage.delete("bar") must beTrue
      storage.read("foo") must beNone
      storage.read("bar") must beNone
    }
  }
}
