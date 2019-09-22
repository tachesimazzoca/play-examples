package controllers.session

import components.storage.Storage

class UserSession (
  storage: Storage,
  namespace: String
) {

  lazy private val namespacePrefix = s"${namespace}."
  lazy private val dataPrefix = s"${namespacePrefix}data."

  def create(): String = storage.create()

  def read(key: String): Map[String, String] =
    storage.read(key).getOrElse(Map.empty)
      .filter(_._1.startsWith(dataPrefix))
      .map(x => x._1.substring(dataPrefix.length()) -> x._2)

  def update(key: String, data: Map[String, String]): Unit = {
    val m = storage.read(key).getOrElse(Map.empty).filter(!_._1.startsWith(namespacePrefix))
    storage.write(key, m ++ data.map(x => s"${dataPrefix}${x._1}" -> x._2))
  }

  def delete(key: String): Unit = {
    val m = storage.read(key).getOrElse(Map.empty).filter(!_._1.startsWith(namespacePrefix))
    storage.write(key, m)
  }
}
