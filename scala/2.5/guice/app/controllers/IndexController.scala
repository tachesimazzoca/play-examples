package controllers

import javax.inject.Inject
import javax.inject.Named
import models._
import play.api.mvc._

class IndexController @Inject()(@Named("session") storage: Storage) extends Controller {
  def index = Action {
    Ok(views.html.index())
  }

  def show(key: String) = Action {
    storage.read(key).map { v =>
      Ok(v)
    }.getOrElse(NotFound)
  }

  def update(key: String, value: String) = Action {
    storage.write(key, value)
    Ok("Count: " + storage.count)
  }

  def remove(key: String) = Action {
    storage.delete(key)
    Ok("Count: " + storage.count)
  }
}
