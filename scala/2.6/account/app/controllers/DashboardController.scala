package controllers

import javax.inject.Inject
import controllers.action.{MemberAction, UserAction}
import models.AccountAccessDao
import play.api.db.Database
import play.api.mvc._

class DashboardController @Inject() (
  cc: ControllerComponents,
  userAction: UserAction,
  memberAction: MemberAction,
  db: Database,
  accountAccessDao: AccountAccessDao
) extends AbstractController(cc) {

  def index = (userAction andThen memberAction) {
    Ok(views.html.dashboard.index())
  }

  def access = (userAction andThen memberAction) { implicit memberRequest =>
    val accountAccessList = db.withConnection { implicit conn =>
      accountAccessDao.selectByAccountId(memberRequest.account.id)
    }
    Ok(views.html.dashboard.access(accountAccessList))
  }
}
