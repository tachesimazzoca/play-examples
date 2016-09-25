package models

import javax.inject.Inject

class BarService @Inject() (systemConfig: SystemConfig) {
  def osName: Option[String] = systemConfig.systemProperty("os.name")
}
