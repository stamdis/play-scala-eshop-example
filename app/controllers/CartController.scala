package controllers

import javax.inject.Inject

import dao.{CartDao, EventDao}
import models.{CartItem, Event}
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.data.Form
import play.api.data.Forms._

import scala.concurrent.ExecutionContext

class CartController @Inject()
(cartDao: CartDao, eventDao: EventDao, cc: ControllerComponents, AuthAction: AuthAction)
(implicit executionContext: ExecutionContext) extends AbstractController(cc) {

  val addToCartForm = Form(
    "id" -> number()
  )

  def getAll = AuthAction.async{ implicit request =>
    val user_id = request.session.get("id").get.toInt
    cartDao.getAllNew(user_id).map(items =>
      Ok(views.html.cart(items)).withSession(request.session - "last" + ("last" -> "/cart"))
    )
  }

  def add(item_id: Int) = AuthAction.async { implicit request =>
    val user_id = request.session.get("id").get.toInt
    val timestamp = java.sql.Timestamp.from(java.time.Instant.now())
    val last_url = request.session.get("last").get
    cartDao.newAdd(CartItem(0, user_id, item_id, 1)).flatMap(_ =>
      eventDao.add(Event(0, user_id, item_id, "addtocart", timestamp)).map(_ =>
        Redirect(last_url).flashing("success" -> "The item has been added to cart.")
      )
    )
  }

  def remove(item_id: Int) = AuthAction.async { implicit request =>
    val user_id = request.session.get("id").get.toInt

    cartDao.delete(user_id, item_id).map(_ =>
      Redirect(routes.CartController.getAll())
    )
  }

  def removeAll = AuthAction.async { implicit request =>
    val user_id = request.session.get("id").get.toInt
    cartDao.deleteAll(user_id).map(_ => Redirect(routes.CartController.getAll()))
  }

  def buy = AuthAction.async { implicit request =>
    val user_id = request.session.get("id").get.toInt
    val timestamp = java.sql.Timestamp.from(java.time.Instant.now())
    cartDao.getAllNew(user_id).flatMap(cartItems =>
    {
      val events = cartItems.flatMap(X => Seq.fill(X._2)(Event(0, user_id, X._1.id, "buy", timestamp)))
      eventDao.addAll(events).flatMap(_ =>
        cartDao.deleteAll(user_id).map(_ =>
          Redirect(routes.UserController.index()).flashing("success" -> "You have issued your order.")
        )
      )
    }
    )
  }
}
