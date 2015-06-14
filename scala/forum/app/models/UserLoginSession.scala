package models

import components.storage.{JDBCStorageEngine, Storage}

class UserLoginSession(storage: Storage) {

  def this() = this(new Storage(new JDBCStorageEngine("session_storage"), "user-login-"))

  def create(userLogin: UserLogin): String = {
    val data = Map(
      "id" -> userLogin.id.toString
    )
    storage.create(data)
  }

  def find(key: String): Option[UserLogin] = {
    for {
      data <- storage.read(key)
      id <- data.get("id")
    } yield UserLogin(id.toLong)
  }

  def delete(key: String): Boolean = storage.delete(key)
}
