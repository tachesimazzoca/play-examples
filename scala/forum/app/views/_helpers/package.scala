package views._helpers

package object tags {
  def config(key: String, default: String = "")
            (implicit app: play.api.Application): String =
    app.configuration.getString(key).getOrElse(default)
}
