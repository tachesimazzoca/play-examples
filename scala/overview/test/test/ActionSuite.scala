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
import play.api.test.{FakeApplication, FakeHeaders, FakeRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ActionSuite extends FunSuite with ScalaFutures with OneAppPerSuite {

  implicit override lazy val app = FakeApplication()

  private case class Logging[A](action: Action[A]) extends Action[A] {
    override def apply(request: Request[A]): Future[Result] = {
      Logger.info(request.toString())
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

  private object AppAction extends ActionBuilder[Request] {
    override def invokeBlock[A](request: Request[A],
                                block: (Request[A]) => Future[Result]): Future[Result] = {
      if (request.remoteAddress.equals("127.0.0.1")) {
        block(request).map { result =>
          result.withHeaders("X-UA-Compatible" -> "Chrome=1")
        }
      } else Future.successful(Forbidden)
    }
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
    whenReady(Enumerator("<foo>bar</foo>".getBytes) |>>> xmlAction(req)) { r =>
      assert(Status.OK === r.header.status)
    }
    whenReady(Enumerator("".getBytes) |>>> xmlAction(req)) { r =>
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
    whenReady(Enumerator("<foo>bar</foo>".getBytes) |>>> xmlAction(req)) { r =>
      assert(Status.OK === r.header.status)
    }
    whenReady(Enumerator("".getBytes) |>>> xmlAction(req)) { r =>
      assert(Status.BAD_REQUEST === r.header.status)
    }
  }

  test("AppAction") {
    val action = (LoggingAction andThen AppAction) {
      Ok("")
    }

    whenReady(action(FakeRequest("GET", "/app", FakeHeaders(), null,
      remoteAddress = "127.0.0.1"))) { r =>
      assert(Status.OK === r.header.status)
    }
    whenReady(action(FakeRequest("GET", "/app", FakeHeaders(), null,
      remoteAddress = "192.168.56.101"))) { r =>
      assert(Status.FORBIDDEN === r.header.status)
    }
  }

  test("localhostOnly") {
    def localhostOnly[A](action: Action[A]): Action[A] =
      Action.async(action.parser) { request =>
        if (request.remoteAddress.startsWith("127.0.0.1")) action(request)
        else Future.successful(Forbidden)
      }

    val action = localhostOnly {
      Action {
        Ok("")
      }
    }

    whenReady(action(FakeRequest("GET", "/", FakeHeaders(), null,
      remoteAddress = "127.0.0.1"))) { r =>
      assert(Status.OK === r.header.status)
    }

    whenReady(action(FakeRequest("GET", "/", FakeHeaders(), null,
      remoteAddress = "192.168.101.101"))) { r =>
      assert(Status.FORBIDDEN === r.header.status)
    }
  }

  test("sessIdCookie") {
    val sessKey = "SESSION_ID"

    def sessIdCookie[A](action: Action[A]): Action[A] =
      Action.async(action.parser) { request =>
        if (request.cookies.get(sessKey).isEmpty) {
          action(request).map { result =>
            result.withCookies(Cookie(sessKey, System.currentTimeMillis.toString))
          }
        } else action(request)
      }

    val action = sessIdCookie {
      Action {
        Ok("")
      }
    }

    val req = FakeRequest()
    whenReady(action(req)) { result =>
      assert(result.header.headers.toMap.get("Set-Cookie").isDefined)
    }
  }

  test("wrapping action") {
    def one[A](action: Action[A]): Action[A] =
      Action.async(action.parser) { request =>
        Logger.info("one: beforeAction")
        val future = action(request)
        Logger.info("one: afterAction")
        future
      }

    def two[A](action: Action[A]): Action[A] =
      Action.async(action.parser) { request =>
        Logger.info("two: beforeAction")
        val future = action(request)
        Logger.info("two: afterAction")
        future
      }

    object OneTwoAction extends ActionBuilder[Request] {
      override def invokeBlock[A](request: Request[A],
                                  block: (Request[A]) => Future[Result]) = block(request)

      override protected def composeAction[A](action: Action[A]): Action[A] = one(two(action))
    }

    val action = OneTwoAction {
      Logger.info("one(two(Action))")
      Ok("")
    }

    whenReady(action(FakeRequest())) { _ =>}
  }

  test("andThen") {
    def trace[A](label: String)(request: Request[A],
                                block: (Request[A]) => Future[Result]) = {
      Logger.info( s"""${label}: beforeAction""")
      val future = block(request)
      Logger.info( s"""${label}: afterAction""")
      future
    }

    object OneAction extends ActionBuilder[Request] {
      override def invokeBlock[A](request: Request[A],
                                  block: (Request[A]) => Future[Result]) =
        trace("OneAction")(request, block)
    }

    object TwoAction extends ActionBuilder[Request] {
      override def invokeBlock[A](request: Request[A],
                                  block: (Request[A]) => Future[Result]) =
        trace("TwoAction")(request, block)
    }

    val action = (OneAction andThen TwoAction) {
      Logger.info("(OneAction andThen TwoAction)")
      Ok("")
    }

    whenReady(action(FakeRequest())) { _ =>}
  }

  test("ActionFilter") {
    object RemoteAddressFilter extends ActionFilter[Request] {
      override protected def filter[A](request: Request[A]) =
        Future.successful {
          if (request.remoteAddress.equals("127.0.0.1")) None
          else Some(Forbidden)
        }
    }

    val action = (Action andThen RemoteAddressFilter) { request =>
      Ok("")
    }

    whenReady(action(FakeRequest("GET", "/", FakeHeaders(), null,
      remoteAddress = "127.0.0.1"))) { r =>
      assert(Status.OK === r.header.status)
    }

    whenReady(action(FakeRequest("GET", "/", FakeHeaders(), null,
      remoteAddress = "192.168.101.101"))) { r =>
      assert(Status.FORBIDDEN === r.header.status)
    }
  }

  test("ActionRefiner") {
    class UserRequest[A](request: Request[A], val sessionId: Option[String])
      extends WrappedRequest[A](request)

    class AccountRequest[A](val user: UserRequest[A], val account: Account)
      extends WrappedRequest[A](user)

    case class Account(id: Option[Long])

    object SessionStorage {
      def read(key: String): Option[Long] = Some(1234L)
    }

    val sessionKey = "SESSION_ID"

    object UserAction extends ActionBuilder[UserRequest]
                              with ActionTransformer[Request, UserRequest] {
      override protected def transform[A](request: Request[A]): Future[UserRequest[A]] =
        Future.successful {
          new UserRequest(request, request.cookies.get(sessionKey).map(_.value))
        }
    }

    val userAction = UserAction { user =>
      Ok("")
    }
    whenReady(userAction(FakeRequest())) { r =>
      assert(Status.OK === r.header.status)
    }

    val AccountAction = new ActionRefiner[UserRequest, AccountRequest] {
      override protected def refine[A](user: UserRequest[A]) = Future.successful {
        (for {
          id <- user.sessionId
          session <- SessionStorage.read(id)
        } yield session)
          .map(id => new AccountRequest(user, Account(Some(id))))
          .toRight(Forbidden)
      }
    }

    val accountAction = (UserAction andThen AccountAction) { account =>
      Ok("")
    }
    whenReady(accountAction(FakeRequest())) { r =>
      assert(Status.FORBIDDEN === r.header.status)
    }
    whenReady(accountAction(FakeRequest()
      .withCookies(Cookie(sessionKey, "0123456789abcdef")))) { r =>
      assert(Status.OK === r.header.status)
    }
  }
}
