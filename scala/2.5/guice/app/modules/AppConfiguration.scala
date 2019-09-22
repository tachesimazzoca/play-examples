package modules

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import com.google.inject.name.Names
import models._

class AppConfiguration extends AbstractModule {
  def configure() = {

    bind(classOf[Storage])
      .annotatedWith(Names.named("session"))
      .to(classOf[MockStorage])

    bind(classOf[SystemConfig])
      .toProvider(classOf[SystemConfigProvider])
      .asEagerSingleton()

    // Build auto-wired factories with AssistedInject
    install(new FactoryModuleBuilder()
      .implement(classOf[Mailer], classOf[MockMailer])
      .build(classOf[Mailer.Factory]))
  }
}
