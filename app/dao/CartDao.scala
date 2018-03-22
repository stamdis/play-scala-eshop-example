package dao

import javax.inject.Inject
import models.{CartItem, Item}
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}

class CartDao @Inject()
(protected val dbConfigProvider: DatabaseConfigProvider)
(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with ItemTrait {

  import profile.api._

  private val CartItems = TableQuery[CartTable]
  private val Items = TableQuery[ItemTable]

  def getAll(user_id: Int): Future[Seq[CartItem]] =
    db.run(CartItems.filter(_.user_id === user_id).result)

  def getAllNew(user_id: Int): Future[Seq[(Item, Int)]] = {
    val query = for (
      cartItem <- CartItems;
      item <- Items if cartItem.user_id === user_id && cartItem.item_id === item.id
    ) yield (item, cartItem.amount)
    db.run(query.result)
  }

  def deleteAll(user_id: Int): Future[Int] =
    db.run(CartItems.filter(_.user_id === user_id).delete)

  def delete(user_id: Int, cartItem_id: Int) =
    db.run{
      CartItems.filter(X => (X.user_id === user_id) && (X.item_id === cartItem_id)).result.flatMap { X =>
        if (X.head.amount == 1)
          CartItems.filter(X => (X.user_id === user_id) && (X.item_id === cartItem_id)).delete.map(_  => ())
        else
          CartItems.filter(X => (X.user_id === user_id) && (X.item_id === cartItem_id)).map(_.amount).update(X.head.amount - 1).map(_  => ())
      }
    }

  def add(cartItem: CartItem): Future[Int] =
    db.run(CartItems += cartItem)

  def newAdd(cartItem: CartItem) =
    db.run{
      CartItems.filter(X => X.user_id === cartItem.user_id && X.item_id === cartItem.item_id).result.flatMap { X =>
        if (X.isEmpty)
          CartItems += cartItem
        else
          CartItems.filter(X => X.user_id === cartItem.user_id && X.item_id === cartItem.item_id).map(_.amount).update(X.head.amount + cartItem.amount)
      }
    }

  private class CartTable(tag: Tag) extends Table[CartItem](tag, "CARTITEM") {

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def user_id = column[Int]("USER_ID")
    def item_id = column[Int]("ITEM_ID")
    def amount = column[Int]("AMOUNT")

    def item = foreignKey("ITEM_FK", item_id, Items)(_.id)

    def * = (id, user_id, item_id, amount) <> (CartItem.tupled, CartItem.unapply)
  }
}