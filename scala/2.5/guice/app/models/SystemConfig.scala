package models

import scala.collection.JavaConversions._

class SystemConfig(properties: Map[String, String]) {
  def property(key: String): Option[String] = properties.get(key)
}

object SystemConfig {
  def apply(): SystemConfig = {
    val properties = System.getProperties.toMap
    properties.foreach(println)
    new SystemConfig(properties)
  }
}
