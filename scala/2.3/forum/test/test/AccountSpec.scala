package test

import models.Account
import org.specs2.mutable._

class AccountSpec extends Specification {

  "Account#hashPassword" should {
    "generate a hashed password with the given salt" in {
      Account.hashPassword("test", Some("salt")) must_==
        Account.Password("salt", "9875cadfaf93c78efff30378dd054cf9a5f4a723")
    }
  }
}
