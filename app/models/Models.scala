package models

import java.sql.Timestamp

case class User(id: Int, email: String)

case class Item(id: Int, price: Double, title: String, description: String, category: Int)

case class CartItem(id: Int, user_id: Int, item_id: Int, amount: Int)

case class Favourite(user_id: Int, item_id: Int)

case class Category(id: Int, parent_id: Int, depth: Int, name: String)

case class Event(id: Int, user_id: Int, item_id: Int, event: String, timestamp: Timestamp)