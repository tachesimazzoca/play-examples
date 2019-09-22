package controllers

import components.storage.Storage
import controllers.action.UserAction
import controllers.cookie.CookieFactory
import controllers.session.UserSessionFactory
import javax.inject.{Inject, Named}
import models._
import models.form._
import play.api.Logger
import play.api.db.Database
import play.api.i18n.I18nSupport
import play.api.mvc._

class AccountController @Inject() (
  cc: ControllerComponents,
  userAction: UserAction,
  db: Database,
  @Named("verificationStorage") verificationStorage: Storage,
  userSessionFactory: UserSessionFactory,
  idSequenceDao: IdSequenceDao,
  accountDao: AccountDao,
  accountAccessDao: AccountAccessDao,
  @Named("accountAccessCookieFactory") accountAccessCookieFactory: CookieFactory
) extends AbstractController(cc) with I18nSupport {

  private val logger = Logger(this.getClass())

  private val accountCreateForm = AccountCreateForm.defaultForm

  lazy private val userLoginSession =
    userSessionFactory.createUserLoginSession()

  def login(returnTo: Option[String]) = userAction { implicit userRequest =>
    val form = AccountLoginForm.defaultForm.fill(
      AccountLoginForm(returnTo = returnTo.filter(isValidReturnTo)))
    Ok(views.html.account.login(form))
  }

  def postLogin = userAction { implicit userRequest =>
    userLoginSession.delete(userRequest.sessionId)
    AccountLoginForm.fromRequest.fold(
      form => {
        BadRequest(views.html.account.login(form))
      },
      data => {
        (for {
          a <- db.withConnection { implicit conn =>
            accountDao.findByEmail(data.email)
          }
          if (a.status == Account.Status.Active && a.password.matches(data.password))
        } yield a).map { account =>

          userLoginSession.update(userRequest.sessionId,
            Map("accountId" -> account.id.toString))

          val accountAccess = db.withTransaction { implicit conn =>
            accountAccessDao.add(
              AccountAccess(AccountAccess.generateCode, account.id,
                userAgent = userRequest.headers.get("User-Agent").getOrElse(""),
                remoteAddress = userRequest.remoteAddress)
            )
          }

          val cookies = if (data.keepMeLoggedIn) {
            Seq(accountAccessCookieFactory.create(accountAccess.code))
          } else {
            Seq.empty[Cookie]
          }

          data.returnTo.filter(isValidReturnTo).map { returnTo =>
            Redirect(returnTo).withCookies(cookies: _*)
          }.getOrElse {
            Redirect(routes.DashboardController.index()).withCookies(cookies: _*)
          }
        }.getOrElse {
          // Re-bind fields in order to add the authorization error
          val m = AccountLoginForm.unbind(data.copy(authorized = false))
          BadRequest(views.html.account.login(AccountLoginForm.defaultForm.bind(m)))
        }
      }
    )
  }

  private def isValidReturnTo(returnTo: String): Boolean =
    returnTo.matches("""^/[-_=~%#&/0-9a-zA-Z\.\-\+\?\:\[\]]{0,255}$""")

  def logout = userAction { implicit request =>
    // Remove the login session
    userLoginSession.delete(request.sessionId)
    // Also remove the "Keep me logged in" cookie
    Redirect(routes.AccountController.login(None)).discardingCookies(
      DiscardingCookie(accountAccessCookieFactory.name))
  }

  def create = userAction { implicit request =>
    Ok(views.html.account.create(accountCreateForm))
  }

  def postCreate = userAction { implicit request =>
    AccountCreateForm.fromRequest.fold(
      form => BadRequest(views.html.account.create(form)),
      data => {
        db.withConnection { implicit conn =>
          accountDao.findByEmail(data.email).map { _ =>
            val formWithError = accountCreateForm.bind(
              accountCreateForm.mapping.unbind(data).updated("uniqueEmail", "false"))
            BadRequest(views.html.account.create(formWithError))
          }.getOrElse {
            val verificationId = verificationStorage.create(accountCreateForm.mapping.unbind(data))
            // TODO: send verificationId to the registered user
            logger.debug(verificationId)
            Redirect(routes.AccountController.verify())
          }
        }
      }
    )
  }

  def verify = userAction {
    Ok(views.html.account.verify())
  }

  def activate = userAction { implicit request =>
    (for {
      code <- request.getQueryString("code")
      params <- verificationStorage.read(code)
      formMayErr = accountCreateForm.bind(params)
      data <- if (formMayErr.hasErrors) None else Some(formMayErr.get)
    } yield {
      db.withConnection { implicit conn =>
        accountDao.findByEmail(data.email)
      }.map { _ =>
        BadRequest
      }.getOrElse {
        val accountId = db.withTransaction { implicit conn =>
          idSequenceDao.nextId(IdSequence.SequenceType.Account)
        }
        val account = Account(accountId, data.email,
          Account.hashPassword(data.password), Account.Status.Active)
        val createdAccount = db.withTransaction { implicit conn =>
          accountDao.create(account)
        }
        Ok(views.html.account.activate(createdAccount))
      }
    }).getOrElse {
      BadRequest
    }
  }

  def error(name: String) = userAction {
    name match {
      case "session" =>
        Forbidden(views.html.account.error.session())
      case "email" =>
        Forbidden(views.html.account.error.email())
      case _ =>
        BadRequest
    }
  }
}
