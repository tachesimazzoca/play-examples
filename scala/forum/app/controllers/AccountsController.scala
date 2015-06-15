package controllers

import models._
import models.form._
import play.api.mvc._

import scala.concurrent.Future

class AccountsController(components: ComponentRegistry) extends Controller {

  private val accountsEntryForm = AccountsEntryForm.defaultForm
  private val accountsLoginForm = AccountsLoginForm.defaultForm

  private val userAction = new UserAction(components)
  private val guestAction = userAction andThen GuestAction

  def errorsSession = guestAction { implicit request =>
    BadRequest(views.html.accounts.errors.session())
  }

  def errorsEmail = guestAction { implicit request =>
    BadRequest(views.html.accounts.errors.email())
  }

  def entry = guestAction { implicit request =>
    Ok(views.html.accounts.entry(accountsEntryForm))
  }

  def postEntry = guestAction { implicit request =>
    accountsEntryForm.bindFromRequest.fold(
      form => BadRequest(views.html.accounts.entry(form)),
      data => {
        components.accountService.findByEmail(data.email).map { _ =>
          val formWithError = accountsEntryForm.bind(
            accountsEntryForm.mapping.unbind(data).updated("uniqueEmail", "false"))
          BadRequest(views.html.accounts.entry(formWithError))
        }.getOrElse {
          val Account.Password(hash, salt) = Account.hashPassword(data.password)
          val sessionKey = components.signUpSession.create(SignUp(data.email, hash, salt))
          val params = SignUpMailer.Params(sessionKey)
          val messageId = SignUpMailer.send(data.email, params)
          Redirect(routes.AccountsController.verify())
        }
      }
    )
  }

  def verify = guestAction { implicit request =>
    Ok(views.html.accounts.verify())
  }

  def activate(code: String) = guestAction { implicit request =>
    components.signUpSession.find(code).map { signUp =>
      components.accountService.findByEmail(signUp.email).map { _ =>
        Redirect(routes.AccountsController.errorsEmail())
      }.getOrElse {
        components.signUpSession.delete(code)
        val account = components.accountService.create(
          Account(0L, signUp.email, Account.Status.ACTIVE),
          Account.Password(signUp.passwordHash, signUp.passwordSalt)
        )
        Ok(views.html.accounts.activate(account))
      }
    }.getOrElse {
      Redirect(routes.AccountsController.errorsSession())
    }
  }

  def logout = guestAction { implicit request =>
    Redirect(routes.AccountsController.login())
  }

  def login = guestAction { implicit request =>
    Ok(views.html.accounts.login(accountsLoginForm))
  }

  def postLogin = userAction { implicit request =>
    accountsLoginForm.bindFromRequest.fold(
      form => BadRequest(views.html.accounts.login(form)),
      data => {
        components.accountService.authenticate(data.email, data.password).map { account =>
          val key = components.userLoginSession.create(UserLogin(account.id))
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
