package models

import javax.inject.Inject

import com.google.inject.assistedinject.Assisted
import play.api.Configuration

trait Mailer {
  def send(headers: Seq[String], body: String): String
}

object Mailer {
  trait Factory {
    def create(name: String): Mailer
  }
}

// AssistedInject generates an implementation of the factory class
// automatically.
class MockMailer @Inject() (config: Configuration,
                            @Assisted name: String) extends Mailer {
  def send(headers: Seq[String], body: String): String = {
    println(config)
    println(headers)
    println(body)
    name + "-message-id"
  }
}
