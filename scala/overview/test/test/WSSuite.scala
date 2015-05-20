package test

import java.net.InetSocketAddress

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.iteratee.Iteratee
import play.api.libs.ws.WS

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

@RunWith(classOf[JUnitRunner])
class WSSuite extends FunSuite with ScalaFutures with OneAppPerSuite {

  private def createHttpServer(host: InetSocketAddress,
                               path: String = "/",
                               status: Int = 200,
                               headers: Map[String, String] = Map.empty,
                               sleep: Long = 0,
                               body: Array[Byte] = Array.empty): HttpServer = {
    val server = HttpServer.create(host, 5)

    server.createContext(path, new HttpHandler() {
      override def handle(t: HttpExchange): Unit = {

        if (sleep > 0) Thread.sleep(sleep)

        val h = t.getResponseHeaders
        headers.foreach { case (k, v) =>
          h.add(k, v)
        }

        withCloseable(t.getResponseBody) { out =>
          if (t.getRequestMethod == "HEAD") t.sendResponseHeaders(status, -1)
          else t.sendResponseHeaders(status, body.length)
          out.write(body)
        }
      }
    })
    server
  }

  private def withCloseable[T, A <: java.io.Closeable](io: A)(block: (A) => T) = {
    try {
      block(io)
    } finally {
      if (io != null) io.close
    }
  }

  // TODO: Load from a test configuration
  lazy val host = new InetSocketAddress("localhost", 9990)

  private def urlString(h: InetSocketAddress, path: String) =
    s"""http://${h.getHostName()}:${h.getPort()}${path}"""

  test("text") {
    val body = "Hello"
    val server = createHttpServer(
      host = host,
      body = body.getBytes
    )

    server.start()

    val holder = WS.url(urlString(host, "/text"))
    val response = Await.result({
      holder.get()
    }, 1.second)
    assert(200 === response.status)
    assert(body === response.body)

    server.stop(0)
  }

  test("xml") {
    val body = <foo>bar</foo>
    val server = createHttpServer(
      host = host,
      headers = Map("Content-Type" -> "application/xml"),
      body = body.toString.getBytes
    )

    server.start()

    val holder = WS.url(urlString(host, "/xml"))
    val response = Await.result({
      holder.get()
    }, 1.second)
    assert(200 === response.status)
    assert(body === response.xml)

    server.stop(0)
  }

  test("timeout") {
    val server = createHttpServer(
      host = host,
      sleep = 500L
    )

    server.start()

    val holder = WS.url(urlString(host, "/timeout")).withRequestTimeout(100)
    intercept[java.util.concurrent.TimeoutException] {
      Await.result({
        holder.get()
      }, 1.second)
    }

    server.stop(0)
  }

  test("stream") {
    val body = "a" * 100
    val server = createHttpServer(
      host = host,
      body = body.getBytes
    )

    server.start()

    val holder = WS.url(urlString(host, "/stream"))
    val (headers, enumerator) = Await.result({
      holder.getStream()
    }, 1.second)
    assert(Some(Seq("100")) === headers.headers.get("Content-Length"))

    val bytes = Await.result({
      (enumerator |>>> Iteratee.consume[Array[Byte]]())
    }, 1.second)
    assert(body.getBytes === bytes)

    server.stop(0)
  }
}
