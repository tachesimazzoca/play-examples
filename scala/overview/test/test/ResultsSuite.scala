package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.http.HeaderNames._
import play.api.http.Status._
import play.api.libs.iteratee._
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class ResultsSuite extends FunSuite {
  test("apply") {
    val result = Result(
      header = ResponseHeader(200, Map(CONTENT_TYPE -> "text/plain")),
      body = Enumerator("Hello World!".getBytes())
    )
    assert("Result(200, Map(Content-Type -> text/plain))" === result.toString)
  }

  test("Status") {
    assert("Result(123, Map(Content-Type -> text/plain; charset=utf-8))"
      === Status(123)("Set a status code manually").toString)
    assert("Result(200, Map(Content-Type -> text/plain; charset=utf-8))"
      === Ok("OK").toString)
    assert("Result(404, Map(Content-Type -> application/xml; charset=utf-8))"
      === NotFound(<message>Page not found</message>).toString)
    assert("Result(503, Map())"
      === ServiceUnavailable.toString)
    assert("Result(303, Map(Location -> /path/to/url))"
      === SeeOther("/path/to/url").toString)
    assert("Result(301, Map(Location -> /path/to/url))"
      === Redirect("/path/to/url", MOVED_PERMANENTLY).toString)
    assert("Result(303, Map(Location -> /path/to/url?foo=bar&foo=baz))"
      === Redirect("/path/to/url", Map("foo" -> Seq("bar", "baz"))).toString)
  }

  test("Writable") {
    import play.api.http.Writeable

    def enumerator[E](in: E)(implicit w: Writeable[E]): Enumerator[Array[Byte]] = {
      Enumerator(w.transform(in))
    }

    val it = Iteratee.fold[Array[Byte], String]("") { (acc, x) =>
      acc + x.map(a => "%c".format(a)).mkString("")
    }

    // Writeable[String]
    val str = "Hello World!"
    (enumerator(str) |>>> it)
      .onComplete(a => assert(Success(str) === a))

    // Writeable[Xml]
    val xml = <foo>bar</foo>
    (enumerator(xml) |>>> it)
      .onComplete(a => assert(Success(xml.toString) === a))
  }
}
