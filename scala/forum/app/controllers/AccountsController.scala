package controllers

import models.{Account, AccountService, SignUp, SignUpMailer, SignUpSession}
import models.form.AccountsEntryForm
import play.api.mvc._
import scala.util.Try

object AccountsController extends Controller {

  val accountsEntryForm = AccountsEntryForm.defaultForm
  val signUpSession = new SignUpSession
  val accountService = new AccountService

  def entry = Action {
    Ok(views.html.accounts.entry(accountsEntryForm))
  }

  def postEntry = Action { implicit request =>
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

  def verify = Action {
    Ok(views.html.accounts.verify())
  }

  def activate(code: String) = Action {
    (for {
      signUp <- signUpSession.find(code)
      account <- Try {
        signUpSession.delete(code)
        accountService.create(
          Account(0L, signUp.email, Account.Status.ACTIVE),
          Account.Password(signUp.passwordHash, signUp.passwordSalt)
        )
      }.toOption
    } yield {
      Ok(views.html.accounts.activate(account))
    }).getOrElse {
      Redirect(routes.AccountsController.errorsSession())
    }
  }

  def errorsSession = Action {
    BadRequest(views.html.accounts.errors.session())
  }

  def errorsEmail = Action {
    BadRequest(views.html.accounts.errors.email())
  }
}
