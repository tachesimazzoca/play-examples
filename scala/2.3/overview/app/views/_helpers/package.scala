package views

import views.html._helpers.customFieldConstructor
import views.html.helper.FieldConstructor

package object _helpers {
  implicit val fieldConstructor = FieldConstructor(customFieldConstructor.f)
}
