package controllers

import javax.inject.Inject

import dao.{EventDao, FavouriteDao}
import models.{Event, Favourite}
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.data.Form
import play.api.data.Forms._

import scala.concurrent.ExecutionContext

class FavouriteController @Inject()
(favouriteDao: FavouriteDao, eventDao: EventDao, cc: ControllerComponents, AuthAction: AuthAction)
(implicit executionContext: ExecutionContext) extends AbstractController(cc) {

  def getAll = AuthAction.async{ implicit request =>
    val user_id = request.session.get("id").get.toInt
    favouriteDao.getAllNew(user_id).map(items =>
      Ok(views.html.favourite(items)).withSession(request.session - "last" + ("last" -> "/favourite"))
    )
  }

  def add(item_id: Int) = AuthAction.async { implicit request =>
    val user_id = request.session.get("id").get.toInt
    val timestamp = java.sql.Timestamp.from(java.time.Instant.now())
    val last_url = request.session.get("last").get
    favouriteDao.add(Favourite(user_id, item_id)).flatMap(_ =>
      eventDao.add(Event(0, user_id, item_id, "addtofavourite", timestamp)).map(_ =>
        Redirect(last_url).flashing("success" -> "The item has been added to favourites.")
      )
    )
  }

  def remove(item_id: Int) = AuthAction.async { implicit request =>
    val user_id = request.session.get("id").get.toInt
    favouriteDao.delete(user_id, item_id).map(_ =>
      Redirect(routes.FavouriteController.getAll())
    )
  }

  def removeAll = AuthAction.async { implicit request =>
    val user_id = request.session.get("id").get.toInt
    favouriteDao.deleteAll(user_id).map(_ =>
      Redirect(routes.FavouriteController.getAll())
    )
  }
}
