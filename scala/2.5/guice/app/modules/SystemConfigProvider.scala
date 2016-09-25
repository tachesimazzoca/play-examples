package modules

import com.google.inject.Provider
import models._

class SystemConfigProvider extends Provider[SystemConfig] {
  override def get(): SystemConfig = SystemConfig()
}
