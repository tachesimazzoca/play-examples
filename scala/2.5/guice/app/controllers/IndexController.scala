package controllers

import javax.inject.{Inject, Named}

import models._
import play.api.mvc._

class IndexController @Inject()(
  clock: Clock, @Named("session") storage: Storage) extends Controller {

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
    Ok("Updated at %s; Count: %d".format(
      new java.util.Date(clock.currentTimeMillis), storage.count))
  }

  def remove(key: String) = Action {
    storage.delete(key)
    Ok("Removed at %s; Count: %d".format(
      new java.util.Date(clock.currentTimeMillis), storage.count))
  }
}
