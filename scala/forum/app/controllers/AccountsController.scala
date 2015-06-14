package controllers

import models.{Account, AccountService}
import models.{SignUp, SignUpMailer, SignUpSession}
import models.{UserLogin, UserLoginSession}
import models.form.AccountsEntryForm
import models.form.AccountsLoginForm
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import scala.concurrent.Future

object AccountsController extends Controller {

  private val SESSION_KEY_USER_LOGIN = "user"

  private val accountService = new AccountService

  private val accountsEntryForm = AccountsEntryForm.defaultForm
  private val signUpSession = new SignUpSession

  private val accountsLoginForm = AccountsLoginForm.defaultForm
  private val userLoginSession = new UserLoginSession

  object GuestAction extends ActionBuilder[Request] {
    override def invokeBlock[A](request: Request[A],
                                block: (Request[A]) => Future[Result]): Future[Result] = {
      block(request).map { result =>
        result.withSession(request.session - SESSION_KEY_USER_LOGIN)
      }
    }
  }

  def errorsSession = GuestAction {
    BadRequest(views.html.accounts.errors.session())
  }

  def errorsEmail = GuestAction {
    BadRequest(views.html.accounts.errors.email())
  }

  def entry = GuestAction {
    Ok(views.html.accounts.entry(accountsEntryForm))
  }

  def postEntry = GuestAction { implicit request =>
    accountsEntryForm.bindFromRequest.fold(
      form => BadRequest(views.html.accounts.entry(form)),
      data => {
        accountService.findByEmail(data.email).map { _ =>
          val formWithError = accountsEntryForm.bind(
              accountsEntryForm.mapping.unbind(data).updated("uniqueEmail", "false"))
          BadRequest(views.html.accounts.entry(formWithError))
        }.getOrElse {
          val Account.Password(hash, salt) = Account.hashPassword(data.password)
          val sessionKey = signUpSession.create(SignUp(data.email, hash, salt))
          val params = SignUpMailer.Params(sessionKey)
          val messageId = SignUpMailer.send(data.email, params)
          Redirect(routes.AccountsController.verify())
        }
      }
    )
  }

  def verify = GuestAction {
    Ok(views.html.accounts.verify())
  }

  def activate(code: String) = GuestAction {
    signUpSession.find(code).map { signUp =>
      accountService.findByEmail(signUp.email).map { _ =>
        Redirect(routes.AccountsController.errorsEmail())
      }.getOrElse {
        signUpSession.delete(code)
        val account = accountService.create(
          Account(0L, signUp.email, Account.Status.ACTIVE),
          Account.Password(signUp.passwordHash, signUp.passwordSalt)
        )
        Ok(views.html.accounts.activate(account))
      }
    }.getOrElse {
      Redirect(routes.AccountsController.errorsSession())
    }
  }

  def logout = GuestAction { request =>
    Redirect(routes.AccountsController.login())
  }

  def login = GuestAction { request =>
    Ok(views.html.accounts.login(accountsLoginForm))
  }

  def postLogin = Action { implicit request =>
    accountsLoginForm.bindFromRequest.fold(
      form => BadRequest(views.html.accounts.login(form)),
      data => {
        accountService.authenticate(data.email, data.password).map { account =>
          val key = userLoginSession.create(UserLogin(account.id))
          Redirect(routes.Application.index()).withSession(SESSION_KEY_USER_LOGIN -> key)
        }.getOrElse {
          val formWithError = accountsLoginForm.fill(data)
              .withError("password", "AccountsLoginForm.error.auth")
          BadRequest(views.html.accounts.login(formWithError))
        }
      }
    )
  }
}
