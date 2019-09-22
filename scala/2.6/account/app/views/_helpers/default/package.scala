package views.html._helpers

import views.html.helper.{FieldConstructor, FieldElements}

package object default {
  implicit val field = new FieldConstructor {
    def apply(elts: FieldElements) = defaultFieldConstructor(elts)
  }
}
