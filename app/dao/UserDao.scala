package dao

import javax.inject.Inject

import models.User
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait UserTrait { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class UserTable(tag: Tag) extends Table[User](tag, "USER") {
    //TODO: Stamatis - Make composite primary key
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def email = column[String]("EMAIL", O.Unique)

    def * = (id, email) <> (User.tupled, User.unapply)
  }
}

class UserDao @Inject()
(protected val dbConfigProvider: DatabaseConfigProvider)
(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with UserTrait {

  import profile.api._

  private val Users = TableQuery[UserTable]

  def all(): Future[Seq[User]] = db.run(Users.result)

  def get(email: String): Future[Option[User]] = db.run(Users.filter(_.email === email).result.headOption)

  def exists(email: String): Future[Boolean]= db.run(Users.filter(_.email === email).exists.result)

  def insert(user: User): Future[Int] = db.run(Users returning Users.map(_.id) += user)//.map { _ => () }

  def insert(email: String): Future[Int] = db.run(Users returning Users.map(_.id) += User(0, email))//.map { _ => () }
}
