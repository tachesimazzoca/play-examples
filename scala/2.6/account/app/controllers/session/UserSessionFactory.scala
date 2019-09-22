package controllers.session

import javax.inject.{Inject, Named}

import components.storage.Storage

class UserSessionFactory @Inject() (
  @Named("sessionStorage") storage: Storage
) {

  def create(namespace: String): UserSession = new UserSession(storage, namespace)

  private val NAMESPACE_USER_LOGIN = "UserLogin"

  def createUserLoginSession(): UserSession = create(NAMESPACE_USER_LOGIN)
}
