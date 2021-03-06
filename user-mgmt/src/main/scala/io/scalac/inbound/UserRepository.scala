package io.scalac.inbound

import java.sql.Timestamp
import java.time.{Instant}

import io.scalac.newspaper.events.SubscribeUser
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global //TODO: inject EC

trait UserRepository {
  def addOrUpdate(u: SubscribeUser): Future[Unit]
  def getAll(): Future[Seq[UserSubscription]]
}

class SlickUserRepository(db: Database) extends UserRepository {
  import SlickUserRepository._

  override def addOrUpdate(u: SubscribeUser): Future[Unit] = {
    val translated = UserSubscription(
      MailRecipient(u.subscriberEmail),
      u.subscriberName,
      Timestamp.from(Instant.now()))


    db.run {
      query.insertOrUpdate(translated)
    }.map{ _ => ()}
  }

  override def getAll(): Future[Seq[UserSubscription]] = db.run {
    query.result
  }
}

object SlickUserRepository {
  implicit val entityTypeMapper = MappedColumnType.base[MailRecipient, String] (
    r => r.to,
    MailRecipient.apply)

  private[this] class UserSubscriptionTable(tag: Tag) extends Table[UserSubscription](tag, "user_subscriptions") {
    def userEmail = column[MailRecipient]("user_email", O.PrimaryKey)
    def userName = column[String]("user_name")
    def timeAdded = column[Timestamp]("time_added")

    def * = (userEmail, userName, timeAdded) <> (UserSubscription.tupled, UserSubscription.unapply)
  }

  private val query = TableQuery[UserSubscriptionTable]
}

case class MailRecipient(to: String) extends AnyVal
case class UserSubscription(userEmail: MailRecipient, userName: String, timeAdded: Timestamp)