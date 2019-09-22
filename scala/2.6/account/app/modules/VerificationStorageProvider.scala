package modules

import components.storage.{DatabaseStorageEngine, Storage}
import components.util.Clock
import javax.inject.{Inject, Named, Provider}
import play.api.db.Database

class VerificationStorageProvider @Inject() (
  clock: Clock,
  db: Database,
  @Named("verificationStorageSettings") settings: Storage.Settings
) extends Provider[Storage] {

  override def get(): Storage =
    new Storage(new DatabaseStorageEngine(clock, db, "verification_storage"), settings)
}
