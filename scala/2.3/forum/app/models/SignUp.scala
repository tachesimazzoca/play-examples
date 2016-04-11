package models

import components.storage.{JDBCStorageEngine, Storage}

case class SignUp(email: String, passwordHash: String, passwordSalt: String)

class SignUpSession(storage: Storage) {

  def this() = this(new Storage(new JDBCStorageEngine("session_storage"), "sign_up-"))

  def create(signUp: SignUp): String = {
    val data = Map(
      "email" -> signUp.email,
      "passwordHash" -> signUp.passwordHash,
      "passwordSalt" -> signUp.passwordSalt
    )
    storage.create(data)
  }

  def find(key: String): Option[SignUp] = {
    for {
      data <- storage.read(key)
      email <- data.get("email")
      passwordHash <- data.get("passwordHash")
      passwordSalt <- data.get("passwordSalt")
    } yield SignUp(email, passwordHash, passwordSalt)
  }

  def delete(key: String): Boolean = storage.delete(key)
}
