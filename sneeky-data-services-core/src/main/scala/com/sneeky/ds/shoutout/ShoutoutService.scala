package com.sneeky.ds.shoutout

import com.netaporter.i18n.ResourceBundle

import scala.concurrent.{ Future, ExecutionContext, future }
import com.sneeky.model._
import com.sneeky.dataAccess.DatabaseSupport
import com.sneeky.ds.{ BasicCrypto, DSConfiguration }
import com.sneeky.ds.framework.{ Dates, Logging }
import akka.actor.ActorRef
import com.sneeky.ds.integration.apple.AppleNotification
import scala.slick.session.Session
import java.util.{ Locale, UUID }

import com.sneeky.ds.integration.amazon.S3Service

import scala.util.Try

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 2:01 PM
 */
trait ShoutoutService {

  def saveData(data : Array[Byte], contentType : String) : Future[String]
  def sendToRecipients(sender : SneekyV2User, url : String, imageText : Option[String], friendIds : Option[String], contentType : String) : Int
  def findAllForUser(user : SneekyV2User, sessionId : UUID) : Future[List[ShoutoutResponse]]
  def updateShoutoutAsViewedForUser(user : SneekyV2User, id : Long) : Future[Int]

}

object ShoutoutService extends DSConfiguration with BasicCrypto {

  def apply(appleActor : ActorRef, s3Service : S3Service)(implicit ec : ExecutionContext) =
    new ShoutoutService with DatabaseSupport with Logging {

      val resourceBundle = ResourceBundle("messages/messages")

      def saveData(data : Array[Byte], contentType : String) : Future[String] = {
        s3Service.saveData(data, contentType)
      }

      val codes = List(
        "\ue415",
        "\ue057",
        "\ue404",
        "\ue011",
        "\ue052",
        "\ue048",
        "\ue531",
        "\ue050",
        "\ue051",
        "\ue528",
        "\ue109",
        "\ue11b",
        "\ue312",
        "\ue112",
        "\ue310",
        "\ue12b",
        "\ue120",
        "\ue33b",
        "\ue33a",
        "\ue34b",
        "\ue10d"
      )

      val randomNumber = scala.util.Random

      private def getNotificationForLocale(localeString : String, sender : SneekyV2User, randomUnicode : String) : String = {

        val localeFromString = com.sneeky.ds.framework.LocaleUtilities.getLocaleFromString(localeString)

        val notificationMessage = (sender.firstName, sender.lastName, sender.username) match {
          case (Some(f), Some(l), _) => {
            val lastInitial = l.charAt(0)
            val name = s"$f $lastInitial."
            resourceBundle.getWithParams("shoutout.pushNotification.sentPhoto", localeFromString, name, randomUnicode)

          }

          case (Some(f), None, _) => resourceBundle.getWithParams("shoutout.pushNotification.sentPhoto", localeFromString, f, randomUnicode)
          case (None, Some(l), _) => resourceBundle.getWithParams("shoutout.pushNotification.sentPhoto", localeFromString, l, randomUnicode)
          case (None, None, u)    => resourceBundle.getWithParams("shoutout.pushNotification.sentPhoto", localeFromString, u, randomUnicode)
          case _                  => s"someone sent you a photo"
        }

        notificationMessage
      }

      private def sendAPNSNotificationsToRecipients(sender : SneekyV2User, recipients : List[SneekyV2User])(implicit session : Session) : Future[Unit] = {
        future {

          val randomUnicode = codes(randomNumber.nextInt(codes.size))
          log.debug(s"Sending APNS notifications for recipients: $recipients")

          recipients.foreach {
            recipient =>

              log.debug(s"about to get the count")
              val unreadMessageCount = shoutoutDao.countUnread(recipient);

              log.debug(s"got the count now getting the tokens $unreadMessageCount")
              val tokens = sessionsDao.findTokensByUserId(recipient.id.get :: Nil)

              log.debug(s"using tokens: $tokens for user: $recipient")

              tokens.getOrElse(recipient.id.get, Set.empty).foreach {
                token =>
                  val localeForSession = sessionsDao.findLocaleForToken(token)
                  val note = AppleNotification(
                    recipient.newMessagePush,
                    Some(token),
                    unreadMessageCount,
                    getNotificationForLocale(localeForSession, sender, randomUnicode))
                  appleActor ! note
              }

          }

        }
      }

      /**
       * Admin only function - do not call this without an admin user
       */
      def sendToAll(sender : SneekyV2User, url : String, text : Option[String], contentType : String, locale : Option[String]) : Int = {
        db.withTransaction { implicit s : Session =>

          val users = locale match {
            case None          => shoutoutUsersDao.findAll()
            case Some("en_US") => shoutoutUsersDao.findAllEnglish()
            case Some(x)       => shoutoutUsersDao.findAllForLocale(x)
          }

          shoutoutDao.insertShoutouts(users, Shoutout(
            None,
            sender.id.get,
            0,
            text.getOrElse(""),
            url,
            false,
            None,
            Dates.nowDT,
            false,
            contentType
          ))
          sendAPNSNotificationsToRecipients(sender, users)

          users.length
        }
      }

      def sendToRecipients(sender : SneekyV2User, url : String, text : Option[String], friendIds : Option[String], contentType : String) : Int = {

        var returnVal = 0

        db.withTransaction { implicit s : Session =>

          // find a unique list of shoutout user for each user id provided
          val uniqueFriendIds : Set[Long] = friendIds match {
            case None     => Set()
            case Some(xs) => xs.split(',').toList.map(x => x.toLong).toSet
          }
          val providedUsers = shoutoutUsersDao.findByIds(uniqueFriendIds)

          // insert a record for each user into the Shoutout table that isn't blocked
          shoutoutDao.insertShoutouts(providedUsers, Shoutout(
            None,
            sender.id.get,
            0,
            text.getOrElse(""),
            url,
            false,
            None,
            Dates.nowDT,
            false,
            contentType
          ))

          // only send notifications to the non-blocked users
          sendAPNSNotificationsToRecipients(sender, providedUsers)

          // return the number of shoutouts that were sent (inserted)
          providedUsers.length
        }
      }

      def findAllForUser(user : SneekyV2User, sessionId : UUID) : Future[List[ShoutoutResponse]] = {
        future {
          db.withSession { implicit s : Session =>
            sessionsDao.updateLastAccessed(sessionId)
          }
        }

        future {
          db.withSession { implicit s : Session =>
            shoutoutUsersDao.updateLastAccessedOperation(user)
          }
        }

        future {
          db.withSession { implicit s : Session =>
            shoutoutDao.findAllForUser(user) map { resp =>
              resp.copy(
                imageUrl = encryptField(resp.imageUrl)
              )
            }
          }
        }
      }

      def updateShoutoutAsViewedForUser(user : SneekyV2User, id : Long) : Future[Int] = {

        future {
          db.withTransaction { implicit s : Session =>
            shoutoutDao.setViewed(user, id)
          }
        }

      }
    }
}
