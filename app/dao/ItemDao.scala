package dao

import javax.inject.Inject

import models.Item
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait ItemTrait extends CategoryTrait { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  private val Categories = TableQuery[CategoryTable]

  class ItemTable(tag: Tag) extends Table[Item](tag, "ITEM") {

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def price = column[Double]("PRICE")
    def title = column[String]("TITLE")
    def description = column[String]("DESCRIPTION")
    def category_id = column[Int]("CATEGORY_ID")

    def item = foreignKey("CATEGORY_FK", category_id, Categories)(_.id)

    def * = (id, price, title, description, category_id) <> (Item.tupled, Item.unapply)
  }
}

class ItemDao @Inject()
(protected val dbConfigProvider: DatabaseConfigProvider)
(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with ItemTrait {

  import profile.api._

  private val Items = TableQuery[ItemTable]

  def all(relatedCategories: Seq[Int]): Future[Seq[Item]] = db.run(
    Items.filter(_.category_id.inSet(relatedCategories.toSet)).result
  )

  def getById(id: Int): Future[Item] = db.run(Items.filter(_.id === id).result.head)
}