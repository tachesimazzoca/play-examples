package controllers.action

import controllers.cookie.CookieFactory
import controllers.routes
import controllers.session.UserSessionFactory
import javax.inject.{Inject, Named}
import models.{Account, AccountAccessDao, AccountDao}
import play.api.db.Database
import play.api.mvc.Results._
import play.api.mvc.{ActionRefiner, Result}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class MemberAction @Inject() (
  db: Database,
  userSessionFactory: UserSessionFactory,
  accountDao: AccountDao,
  accountAccessDao: AccountAccessDao,
  @Named("accountAccessCookieFactory") accountAccessCookieFactory: CookieFactory
)(implicit val executionContext: ExecutionContext) extends ActionRefiner[UserRequest, MemberRequest] {

  override protected def refine[A](
    request: UserRequest[A]
  ): Future[Either[Result, MemberRequest[A]]] = Future.successful {

    db.withConnection { implicit conn =>
      val accountIdOpt = userSessionFactory.createUserLoginSession()
        .read(request.sessionId)
        .get("accountId")
        .flatMap(x => Try(x.toLong).toOption)
        .orElse {
          for {
            cookie <- request.cookies.get(accountAccessCookieFactory.name)
            a <- accountAccessDao.find(cookie.value)
          } yield a.accountId
        }

      val accountOpt = for {
        accountId <- accountIdOpt
        a <- accountDao.find(accountId) if a.status == Account.Status.Active
      } yield a

      accountOpt.map { account =>
        Right(new MemberRequest(account, request))
      }.getOrElse {
        val returnTo =
          if (request.method == "GET") Some(request.uri)
          else None
        Left(Redirect(routes.AccountController.login(returnTo)))
      }
    }
  }
}
