package views.html._helpers

import views.html.helper.{FieldConstructor, FieldElements}

package object main {
  implicit val field = new FieldConstructor {
    def apply(elts: FieldElements) = mainFieldConstructor(elts)
  }
}
