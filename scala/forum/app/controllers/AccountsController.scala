package controllers

import models.form.AccountsEntryForm
import models.form.AccountsLoginForm
import models.Account
import models.AccountService
import models.SignUp
import models.SignUpMailer
import models.SignUpSession
import models.UserLogin
import models.UserLoginSession
import play.api.mvc._

import scala.concurrent.Future

object AccountsController extends Controller {

  private val accountService = UserAction.accountService

  private val accountsEntryForm = AccountsEntryForm.defaultForm
  private val signUpSession = new SignUpSession

  private val accountsLoginForm = AccountsLoginForm.defaultForm
  private val userLoginSession = UserAction.userLoginSession

  def errorsSession = (UserAction andThen GuestAction) { implicit request =>
    BadRequest(views.html.accounts.errors.session())
  }

  def errorsEmail = (UserAction andThen GuestAction) { implicit request =>
    BadRequest(views.html.accounts.errors.email())
  }

  def entry = (UserAction andThen GuestAction) { implicit request =>
    Ok(views.html.accounts.entry(accountsEntryForm))
  }

  def postEntry = (UserAction andThen GuestAction) { implicit request =>
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

  def verify = (UserAction andThen GuestAction) { implicit request =>
    Ok(views.html.accounts.verify())
  }

  def activate(code: String) = (UserAction andThen GuestAction) { implicit request =>
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

  def logout = (UserAction andThen GuestAction) { implicit request =>
    Redirect(routes.AccountsController.login())
  }

  def login = (UserAction andThen GuestAction) { implicit request =>
    Ok(views.html.accounts.login(accountsLoginForm))
  }

  def postLogin = UserAction { implicit request =>
    accountsLoginForm.bindFromRequest.fold(
      form => BadRequest(views.html.accounts.login(form)),
      data => {
        accountService.authenticate(data.email, data.password).map { account =>
          val key = userLoginSession.create(UserLogin(account.id))
          Redirect(routes.DashboardController.index())
            .withSession(UserAction.SESSION_KEY_USER_LOGIN -> key)
        }.getOrElse {
          val formWithError = accountsLoginForm.fill(data)
            .withError("password", "AccountsLoginForm.error.auth")
          BadRequest(views.html.accounts.login(formWithError))
        }
      }
    )
  }
}
