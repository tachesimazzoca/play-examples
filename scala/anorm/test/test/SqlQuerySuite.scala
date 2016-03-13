package test

import anorm._
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import test.models.User

@RunWith(classOf[JUnitRunner])
class SqlQuerySuite extends FunSuite {

   test("Implicit conversion to SimpleSql") {
     val sq = SQL("SELECT * FROM users WHERE id = {id}")
     assert(sq.isInstanceOf[SqlQuery])
     val ss = sq.on('id -> 1)
     assert(ss.isInstanceOf[SimpleSql[Row]])
   }

   test("Inspect the advantage of holding parsed SqlQuery") {
     User.withInMemoryTable(Nil) { implicit conn =>

       val stmt = "SELECT email FROM users WHERE id = {id}"
       val sql = SQL(stmt)
       val parser = SqlParser.str("email").*

       var start = 0L

       start = System.currentTimeMillis
       for (n <- 1 to 10000) {
         SQL(stmt).on('id -> n).as(parser)
       }
       println("Parse statement each call: " + (System.currentTimeMillis - start))

       start = System.currentTimeMillis
       for (n <- 1 to 10000) {
         sql.on('id -> n).as(parser)
       }
       println("Prepare SqlQuery: " + (System.currentTimeMillis - start))
     }
   }
 }
