import play.api._

object FakeGlobal extends GlobalSettings {

  override def onStart(app: Application) {
    //Logger.info("Application has started")
  }

  override def onStop(app: Application) {
    Thread.sleep(100L)
    //Logger.info("Application shutdown...")
  }

}
