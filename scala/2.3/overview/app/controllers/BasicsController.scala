package controllers

import play.api._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import play.api.mvc._

import scala.util.Try

object BasicsController extends Controller {
  // The charset is handled automatically via the play.api.mvc.Codec type class.
  // Just import an implicit instance of play.api.mvc.Codec in the current scope
  // to change the charset that will be used by all operations.
  implicit val charset = Codec.javaSupported("utf-8")

  def todo = TODO

  def forbidden = Action {
    // Status(FORBIDDEN)("...")
    Forbidden("Error 403 Forbidden")
  }

  def notfound = Action {
    // Status(NOT_FOUND)("...")
    NotFound("Error 404 NotFound")
  }

  def redirect = Action {
    //var result = SimpleResult(
    //  ResponseHeader(
    //    SEE_OTHER,
    //    Map(LOCATION -> routes.Basics.plain().toString)),
    //    Enumerator("")
    //)
    val result = Redirect(routes.BasicsController.plain())
    println(result)
    result
  }

  def plain = Action {
    // "text/plain" as default for a String value.
    Ok("<pre>This is a text/plain response.</pre>")
  }

  def xml = Action {
    // "text/xml" as default for a XML literal.
    var result = Ok(<response>This is a text/xml response.</response>)
    println(result)
    result
  }

  def html = Action {
    // Set "text/html" manually by using play.api.mvc.Result#as
    var result = Ok("<strong>This is a text/html response. 日本語表示</strong>").as(HTML)
    println(result)
    result
  }

  def session = Action { request =>
    val tsOpt = request.session.get("timestamp")
    val sess = Session(Map("timestamp" -> System.currentTimeMillis.toString()))
    val msg =
      if (tsOpt.isDefined)
        "Last visited at %s".format(new java.util.Date(tsOpt.get.toLong))
      else
        "Hello Session!"
    Ok(msg).withSession(sess)
  }

  def application = Action { request =>
    Ok(views.html.basics.application(Play.current))
  }

  def request = Action { request =>
    Ok(views.html.basics.request(request))
  }

  def messages = Action {
    Ok(views.html.basics.messages(
      play.api.i18n.Messages.messages(Play.current)))
  }

  def download = Action {
    Try(getClass.getResource("/data/a.txt")).map { url =>
      val file = new java.io.File(url.getPath())
      //Result(
      //  header = ResponseHeader(OK, Map(
      //    CONTENT_DISPOSITION -> "attachment; filename=download-a.txt",
      //    CONTENT_TYPE -> "application/octet-stream",
      //    CONTENT_LENGTH -> file.length().toString())),
      //  body = Enumerator.fromFile(file)
      //)
      Ok.sendFile(
        content = file,
        inline = false,
        fileName = { f => "download-" ++ f.getName()},
        onClose = { () =>
          println("handled onClose function")
        }
      )
    }.getOrElse {
      Forbidden("This action doesn't support a packaged .jar")
    }
  }

  def stream = Action {
    val input = getClass.getResourceAsStream("/data/a.txt")
    Ok.chunked(Enumerator.fromStream(input)).withHeaders(
      CONTENT_DISPOSITION -> "attachment; filename=download-a.txt",
      CONTENT_TYPE -> "application/octet-stream"
    )
  }
}
