package modules

import components.storage.{DatabaseStorageEngine, Storage}
import components.util.Clock
import javax.inject.{Inject, Named, Provider}
import play.api.db.Database

class SessionStorageProvider @Inject() (
  clock: Clock,
  db: Database,
  @Named("sessionStorageSettings") settings: Storage.Settings
) extends Provider[Storage] {

  override def get(): Storage =
    new Storage(new DatabaseStorageEngine(clock, db, "session_storage"), settings)
}
