package modules

import com.google.inject.Provider
import controllers.cookie.CookieFactory

class CookieFactoryProvider(
  name: String,
  maxAge: Option[Int] = None,
  path: String = "/",
  domain: Option[String] = None,
  secure: Boolean = false,
  httpOnly: Boolean = true
) extends Provider[CookieFactory] {

  override def get(): CookieFactory = new CookieFactory(
    name, maxAge, path, domain, secure, httpOnly)
}
