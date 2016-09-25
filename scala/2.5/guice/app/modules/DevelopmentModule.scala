package modules

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import models._

class DevelopmentModule extends AbstractModule {
  def configure() = {

    bind(classOf[Storage])
      .annotatedWith(Names.named("session"))
      .to(classOf[MockStorage])

    bind(classOf[SystemConfig])
      .toProvider(classOf[SystemConfigProvider])
      .asEagerSingleton()
  }
}
