package models

class SystemClock extends Clock {
  override def currentTimeMillis: Long = System.currentTimeMillis
}
