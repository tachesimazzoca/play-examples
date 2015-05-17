package controllers

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

object LoggingFilter extends Filter {
  override def apply(f: (RequestHeader) => Future[Result])
                    (rh: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis
    f(rh).map { result =>
      val ua = rh.headers.get("User-Agent").getOrElse("-")
      val ms = System.currentTimeMillis - startTime
      val line = s"""${startTime} ${rh.remoteAddress} ${rh.method} ${rh.uri} ${ua} ${ms}"""
      Logger.info(line)
      result
    }
  }
}
