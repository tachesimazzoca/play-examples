package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.test._

@RunWith(classOf[JUnitRunner])
class RequestSuite extends FunSuite {
  test("FakeRequest.appy()") {
    val r = FakeRequest()
    assert("GET" === r.method)
    assert("/" === r.path)
  }

  test("FakeRequest.apply(method, path)") {
    val r = FakeRequest("POST", "/create")
    assert("POST" === r.method)
    assert("/create" === r.path)
  }

  test("XML FakeRequest") {
    val body = <foo>bar</foo>
    val headers = FakeHeaders(Seq("Content-Type" -> Seq("application/xml")))
    val r = FakeRequest("POST", "/xml", headers, body)
    assert("POST" === r.method)
    assert("/xml" === r.path)
    assert(headers === r.headers)
    assert(body === r.body)
  }

  test("custom FakeRequest") {
    val body = (123, "Foo")
    val ua = "Superb Bot/1.2"
    val r = FakeRequest("PUT", "/update")
      .withHeaders("User-Agent" -> ua)
      .withBody(body)
    assert("PUT" === r.method)
    assert("/update" === r.path)
    assert(None === r.headers.get("nokey"))
    assert(Some(ua) === r.headers.get("User-Agent"))
    assert(body === r.body)
  }
}
