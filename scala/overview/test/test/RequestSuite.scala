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

  test("custom FakeRequest") {
    val body = (123, "Foo")
    val h = Map("User-Agent" -> "Superb Bot/1.2")
    val headers = FakeHeaders(h.toSeq.map(t => (t._1, Seq(t._2))))
    val r = new FakeRequest("PUT", "/update", headers, body)
    assert("PUT" === r.method)
    assert("/update" === r.path)
    assert(headers === r.headers)
    assert(None === r.headers.get("nokey"))
    assert(h.get("User-Agent") === r.headers.get("User-Agent"))
    assert(body === r.body)
  }
}
