package controllers

import javax.inject.Inject

import dao._
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms._

import scala.concurrent.{ExecutionContext, Future}
import models.User

class UserController @Inject()
(userDao: UserDao, itemDao: ItemDao, cartDao: CartDao, eventDao: EventDao, categoryDao: CategoryDao, cc: ControllerComponents, AuthAction: AuthAction)
(implicit executionContext: ExecutionContext) extends AbstractController(cc) {

  def index(category_id: Int) = AuthAction.async { implicit request =>
      categoryDao.getMenu(category_id).flatMap(categories =>
        categoryDao.getAllChilds(category_id).flatMap(relatedCategories =>
          itemDao.all(relatedCategories).map( items =>
                Ok(views.html.index2(items, categories, category_id)).withSession(request.session - "category" + ("category" -> category_id.toString) - "last" + ("last" -> "/items?category=".concat(category_id.toString)))
              )
          )
      )
  }

  def admin = AuthAction.async( implicit request =>
    userDao.all().flatMap { users =>
      eventDao.getEverything.map( events =>
        Ok(views.html.admin(users, events)).withSession(request.session - "last" + ("last" -> "/admin"))
      )
    }
  )

  /*val userForm = Form(
    mapping(
      "id" -> number(),
      "email" -> text()
    )(User.apply)(User.unapply)
  )

  def insertUser = AuthAction.async { implicit request =>
    val user: User = userForm.bindFromRequest.get
    userDao.exists(user.email).map{check =>
      if(!check)
        userDao.insert(user)
      Redirect(routes.UserController.index())
    }
  }*/

  def showLogin = Action { implicit request =>
    if(request.session.get("connected").isEmpty)
      Ok(views.html.login())
    else
      Redirect(routes.UserController.index())
  }

  val loginForm = Form("email" -> text)

  def login = Action.async { implicit request =>
    val email = loginForm.bindFromRequest().get

    userDao.get(email).flatMap{userOption =>
      if(userOption.isEmpty)
        userDao.insert(email).map(id =>
          Redirect(routes.UserController.index()).withSession(request.session + ("connected" -> email) + ("id" -> id.toString)))
      else
        Future(Redirect(routes.UserController.index()).withSession(request.session + ("connected" -> email) + ("id" -> userOption.get.id.toString)))
    }
  }

  def logout = AuthAction { implicit request =>
    Redirect(routes.UserController.showLogin()).withNewSession
  }

  //TODO: Stamatis - Why not async
}
