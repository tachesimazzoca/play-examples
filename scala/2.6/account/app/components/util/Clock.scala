package components.util

import com.google.inject.ImplementedBy

@ImplementedBy(classOf[SystemClock])
trait Clock {
  def currentTimeMillis: Long
}
