package controllers

import play.api.mvc.Results._


import javax.inject.Inject
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}


class AuthAction @Inject()
(parser: BodyParsers.Default)
(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {

  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    if (request.session.get("connected").isEmpty) {
      Future.successful(Redirect("/login"))
    } else {
      block(request)
    }
  }
}