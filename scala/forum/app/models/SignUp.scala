package models

import components.storage.{JDBCStorageEngine, Storage}

case class SignUp(email: String)

class SignUpSession(storage: Storage) {

  def this() = this(new Storage(new JDBCStorageEngine("session_storage"), "sign_up-"))

  def create(signUp: SignUp): String = {
    val data = Map("email" -> signUp.email)
    storage.create(data)
  }

  def find(key: String): Option[SignUp] = {
    for {
      data <- storage.read(key)
      email <- data.get("email")
    } yield SignUp(email)
  }
}
