package test

import java.net.InetSocketAddress

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.ws.WS

import scala.concurrent.Await
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class WSSuite extends FunSuite with ScalaFutures with OneAppPerSuite {

  private def createHttpServer(host: InetSocketAddress,
                               path: String = "/",
                               status: Int = 200,
                               headers: Map[String, String] = Map.empty,
                               body: Array[Byte] = Array.empty): HttpServer = {
    val server = HttpServer.create(host, 5)

    server.createContext(path, new HttpHandler() {
      override def handle(t: HttpExchange): Unit = {
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

  test("WS") {
    val body = "Hello"
    val server = createHttpServer(
      host = host,
      body = body.getBytes
    )
    server.start()
    val holder = WS.url(urlString(host, "/"))
    val response = Await.result({
      holder.get()
    }, 1.second)
    assert(200 === response.status)
    assert("Hello" === response.body)
    server.stop(0)
  }
}
