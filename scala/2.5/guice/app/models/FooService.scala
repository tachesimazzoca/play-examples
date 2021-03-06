package models

import javax.inject.Inject

class FooService @Inject() (systemConfig: SystemConfig) {
  def fileEncoding: Option[String] = systemConfig.property("file.encoding")
}
