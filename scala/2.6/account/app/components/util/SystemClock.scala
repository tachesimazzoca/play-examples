package components.util

import javax.inject.Singleton

@Singleton
class SystemClock extends Clock {
  def currentTimeMillis: Long = System.currentTimeMillis
}
