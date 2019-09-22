package controllers.cookie

import play.api.mvc.Cookie

class CookieFactory(
  val name: String,
  val maxAge: Option[Int] = None,
  val path: String = "/",
  val domain: Option[String] = None,
  val secure: Boolean = false,
  val httpOnly: Boolean = true
) {

  def create(value: String) = Cookie(
    name, value, maxAge, path, domain, secure, httpOnly)
}
