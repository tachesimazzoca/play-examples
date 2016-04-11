import controllers.ComponentRegistry
import controllers.LoggingFilter
import play.api.mvc._
import models._

object Global extends WithFilters(LoggingFilter) {

  private val components = new ComponentRegistry {
    lazy val accountService = new AccountService
    lazy val userLoginSession = new UserLoginSession
    lazy val signUpSession = new SignUpSession
  }

  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    controllerClass.getConstructor(classOf[controllers.ComponentRegistry])
      .newInstance(components)
  }
}
