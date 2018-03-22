package dao

import javax.inject.Inject
import models.{Category, Item}
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}

trait CategoryTrait { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  private val Categories = TableQuery[CategoryTable]

  class CategoryTable(tag: Tag) extends Table[Category](tag, "CATEGORY") {

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def parent_id = column[Int]("PARENT_ID")
    def depth = column[Int]("DEPTH")
    def name = column[String]("NAME")


    def parent = foreignKey("PARENT_FK", parent_id, Categories)(_.id)

    def * = (id, parent_id, depth, name) <> (Category.tupled, Category.unapply)
  }
}

class CategoryDao @Inject()
(protected val dbConfigProvider: DatabaseConfigProvider)
(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with CategoryTrait {

  import profile.api._

  private val Categories = TableQuery[CategoryTable]

  def all(): Future[Seq[Category]] = db.run(Categories.result)

  def getChilds(parent_id: Int): Future[Seq[Category]] = db.run(
    Categories.filter(_.parent_id === parent_id).result
  )

  def findParentId(id: Int): Future[Int] = db.run(
    Categories.filter(_.id === id).result.head.map(_.parent_id)
  )

  def getAllChilds(id: Int): Future[Seq[Int]] = {

    def getAllChildsAcc(left: Seq[Int], all: Seq[Int]): Future[Seq[Int]] = {
      if (left.isEmpty)
        Future(all)
      else
        getChilds(left.head).flatMap(childs =>
          getAllChildsAcc(left.tail ++ childs.map(_.id), all :+ left.head)
        )
    }

    getAllChildsAcc(Seq(id), Seq())
  }

  def getMenu(id: Int): Future[Seq[Category]] = {

    def getMenuAcc(id: Int, childs: Seq[Category]): Future[Seq[Category]] = {
      if (id == 0)
        Future(childs)
      else
        findParentId(id).flatMap(parent_id =>
          getChilds(parent_id).flatMap { parentChilds =>
            val index = parentChilds.map(_.id).indexOf(id) + 1
            val lists = parentChilds.splitAt(index)
            getMenuAcc(parent_id, lists._1 ++ childs ++ lists._2)
          }
        )
    }

    getChilds(id).flatMap (children =>
      getMenuAcc(id, children)
    )
  }
}