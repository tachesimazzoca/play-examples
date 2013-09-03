package test

import java.io.File
import com.typesafe.config.{ConfigFactory, ConfigParseOptions}
import scala.collection.JavaConversions._

object Helpers {
  def inTest: Map[String, String] = {
    val config = ConfigFactory.parseFile(
      new File("conf/application.test.conf"),
      ConfigParseOptions.defaults().setAllowMissing(false)
    )
    config.entrySet.map { entry =>
      val k = entry.getKey
      (k, config.getString(k))
    }.toMap
  }
}
