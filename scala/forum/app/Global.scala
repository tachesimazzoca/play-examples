import controllers.LoggingFilter
import play.api.mvc._

object Global extends WithFilters(LoggingFilter) {
}
