package models

import components.storage.{JDBCStorageEngine, Storage}

case class Signup(email: String)

class SignupService(storage: Storage) {

  def this() = this(new Storage(new JDBCStorageEngine("session_storage"), "signup-"))

  def create(signup: Signup): String = {
    val data = Map("email" -> signup.email)
    storage.create(data)
  }

  def find(key: String): Option[Signup] = {
    for {
      data <- storage.read(key)
      email <- data.get("email")
    } yield Signup(email)
  }
}
