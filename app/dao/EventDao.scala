package dao

import javax.inject.Inject

import models.Event
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}

class EventDao @Inject()
(protected val dbConfigProvider: DatabaseConfigProvider)
(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val Events = TableQuery[EventTable]

  def getEverything: Future[Seq[Event]] =
    db.run(Events.result)

  def getAll(user_id: Int): Future[Seq[Event]] =
    db.run(Events.filter(_.user_id === user_id).result)

  def add(event: Event): Future[Int] =
    db.run(Events += event)

  def addAll(events: Seq[Event]): Future[Option[Int]] =
    db.run(Events ++= events)

  private class EventTable(tag: Tag) extends Table[Event](tag, "EVENT") {

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def user_id = column[Int]("USER_ID")
    def item_id = column[Int]("ITEM_ID")
    def event = column[String]("EVENT")
    def timestamp = column[java.sql.Timestamp]("TIMESTAMP")

    def * = (id, user_id, item_id, event, timestamp) <> (Event.tupled, Event.unapply)
  }
}