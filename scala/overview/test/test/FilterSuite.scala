package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.iteratee.Iteratee
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class FilterSuite extends FunSuite with ScalaFutures with OneAppPerSuite {
  private val bytesConsumer = Iteratee.consume[Array[Byte]]()

  private def byteToStr(bytes: Array[Byte]): String = bytes.map(_.toChar).mkString("")

  test("Filter") {
    val sessionKey = "SESSION_ID"

    val filter = Filter { (f, rh) =>
      f(rh).map { result =>
        if (rh.cookies.get(sessionKey).isEmpty) {
          result.withCookies(Cookie(sessionKey, java.util.UUID.randomUUID().toString))
        }
        else result
      }
    }

    val action = Action { request =>
      val sessionId = request.cookies.get(sessionKey).map(_.value).getOrElse("")
      Ok("sessionId: " ++ sessionId)
    }

    whenReady(for {
      result <- filter(action)(FakeRequest()).run
      bytes <- result.body |>>> bytesConsumer
    } yield (result, byteToStr(bytes))) { t =>
      assert(t._1.header.headers.get("Set-Cookie").isDefined)
      assert("sessionId: " === t._2)
    }

    val req = FakeRequest().withCookies(Cookie(sessionKey, "deadbeef"))
    whenReady(for {
      result <- filter(action)(req).run
      bytes <- result.body |>>> bytesConsumer
    } yield (result, byteToStr(bytes))) { t =>
      assert(t._1.header.headers.get("Set-Cookie").isEmpty)
      assert("sessionId: deadbeef" === t._2)
    }
  }
}
