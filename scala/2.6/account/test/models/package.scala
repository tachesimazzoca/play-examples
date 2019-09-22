package models

import play.api.db.evolutions._
import play.api.db.{Database, Databases}

package object test {
  def withTestDatabase[T](dbName: String = "default")(block: Database => T): T = {
    Databases.withDatabase(
      driver = "org.h2.Driver",
      url = "jdbc:h2:mem:play;MODE=MYSQL",
      name = dbName
    ) { database =>
      Evolutions.withEvolutions(database) {
        block(database)
      }
    }
  }
}
