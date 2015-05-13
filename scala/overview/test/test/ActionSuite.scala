package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import play.api.http.ContentTypes._
import play.api.libs.iteratee.Iteratee
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.FakeRequest

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ActionSuite extends FunSuite with ScalaFutures {

  class LoggingAction(buffer: ArrayBuffer[Any]) extends ActionBuilder[Request] {
    override def invokeBlock[A](request: Request[A],
                                block: (Request[A]) => Future[Result]): Future[Result] = {
      buffer.append(request)
      block(request)
    }
  }

  val bytesConsumer = Iteratee.consume[Array[Byte]]()

  test("apply") {
    val body = "Hello"
    val action = Action {
      Ok(body).as(TEXT)
    }

    whenReady(for {
      r <- action(FakeRequest())
      xs <- r.body |>>> bytesConsumer
    } yield xs) { bytes =>
      assert("Hello".getBytes === bytes)
    }
  }

  test("LoggingAction") {
    val buffer = new ArrayBuffer[Any]()

    val action = new LoggingAction(buffer).apply {
      Ok("")
    }

    val req = FakeRequest("GET", "/foo")
    whenReady(action(req)) { _ =>
      assert(req === buffer(0))
    }
  }
}
