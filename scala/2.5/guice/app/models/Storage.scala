package models

trait Storage {
  def read(key: String): Option[String]

  def write(key: String, value: String): Unit

  def delete(key: String): Unit

  def count: Long
}
