package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatestplus.play.OneAppPerSuite
import play.api.Logger
import play.api.http.ContentTypes._
import play.api.http.Status
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.{FakeApplication, FakeRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ActionSuite extends FunSuite with ScalaFutures with OneAppPerSuite {

  implicit override lazy val app = FakeApplication()

  private case class Logging[A](action: Action[A]) extends Action[A] {
    override def apply(request: Request[A]): Future[Result] = {
      Logger.info(request.toString)
      action(request)
    }

    lazy val parser = action.parser
  }

  private object LoggingAction extends ActionBuilder[Request] {
    override def invokeBlock[A](request: Request[A],
                                block: (Request[A]) => Future[Result]): Future[Result] = {
      block(request)
    }

    override protected def composeAction[A](action: Action[A]): Action[A] = new Logging(action)
  }

  private val bytesConsumer = Iteratee.consume[Array[Byte]]()

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

  test("Logging") {
    val req = FakeRequest("POST", "/logging")

    val action = Logging {
      Action {
        Ok("")
      }
    }
    val xmlAction = Logging {
      Action(parse.tolerantXml) { request =>
        Ok("")
      }
    }

    whenReady(action(req)) { r =>
      assert(Status.OK === r.header.status)
    }
    whenReady(Enumerator("<foo>bar</foo>".getBytes()) |>>> xmlAction(req)) { r =>
      assert(Status.OK === r.header.status)
    }
    whenReady(Enumerator("".getBytes()) |>>> xmlAction(req)) { r =>
      assert(Status.BAD_REQUEST === r.header.status)
    }
  }

  test("LoggingAction") {
    val req = FakeRequest("POST", "/loggingAction")

    val action = LoggingAction {
      Ok("")
    }
    val xmlAction = LoggingAction(parse.tolerantXml) { request =>
      Ok("")
    }

    whenReady(action(req)) { r =>
      assert(Status.OK === r.header.status)
    }
    whenReady(Enumerator("<foo>bar</foo>".getBytes()) |>>> xmlAction(req)) { r =>
      assert(Status.OK === r.header.status)
    }
    whenReady(Enumerator("".getBytes()) |>>> xmlAction(req)) { r =>
      assert(Status.BAD_REQUEST === r.header.status)
    }
  }
}
