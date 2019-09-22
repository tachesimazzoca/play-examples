package views._helpers

package object tags {
  def config(key: String, default: String = "")
            (implicit app: play.api.Application): String =
    app.configuration.getOptional[String](key).getOrElse(default)
}
