package modules

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import components.storage.Storage
import components.util.Chances
import controllers.cookie.CookieFactory

class ApplicationModule extends AbstractModule {
  def configure() = {
    // sessionIdKey
    bind(classOf[String])
      .annotatedWith(Names.named("sessionIdKey"))
      .toInstance("PLAYSESSID")

    // sessionStorage
    bind(classOf[Storage.Settings])
      .annotatedWith(Names.named("sessionStorageSettings"))
      .toInstance(Storage.Settings(
        gcMaxLifetime = Some(1440L), gcChance = Chances.random(1, 100)))
    bind(classOf[Storage])
      .annotatedWith(Names.named("sessionStorage"))
      .toProvider(classOf[SessionStorageProvider])
      .asEagerSingleton()

    // verificationStorage
    bind(classOf[Storage.Settings])
      .annotatedWith(Names.named("verificationStorageSettings"))
      .toInstance(Storage.Settings(
        gcMaxLifetime = Some(1440L), gcChance = Chances.random(1, 100)))
    bind(classOf[Storage])
      .annotatedWith(Names.named("verificationStorage"))
      .toProvider(classOf[VerificationStorageProvider])
      .asEagerSingleton()

    // accountAccessCookieFactory
    bind(classOf[CookieFactory])
      .annotatedWith(Names.named("accountAccessCookieFactory"))
      .toProvider(new CookieFactoryProvider(
        name = "_ACCOUNT_ACCESS_", maxAge = Some(86400 * 90)))
      .asEagerSingleton()
  }
}
