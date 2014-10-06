package com.shoutout.ds.shoutout

import com.netaporter.i18n.ResourceBundle

import scala.concurrent.{ Future, ExecutionContext, future }
import com.shoutout.model._
import com.shoutout.dataAccess.DatabaseSupport
import com.shoutout.ds.{ BasicCrypto, DSConfiguration }
import com.shoutout.ds.framework.{ Dates, Logging }
import akka.actor.ActorRef
import com.shoutout.ds.integration.apple.AppleNotification
import scala.slick.session.Session
import java.util.{ Locale, UUID }

import com.shoutout.ds.integration.amazon.S3Service

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
  def sendToRecipients(sender : ShoutoutUser, url : String, imageText : Option[String], groupIds : Option[String], friendIds : Option[String], contentType : String) : Int
  def findAllForUser(user : ShoutoutUser, sessionId : UUID) : Future[List[ShoutoutResponse]]
  def updateShoutoutAsViewedForUser(user : ShoutoutUser, id : Long) : Future[Int]

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

      private def sendAPNSNotificationsToRecipients(sender : ShoutoutUser, recipients : List[ShoutoutUser])(implicit session : Session) : Future[Unit] = {
        future {

          val randomUnicode = codes(randomNumber.nextInt(codes.size))
          val notificationMessage = (sender.firstName, sender.lastName, sender.username) match {
            case (Some(f), Some(l), _) => {
              val lastInitial = l.charAt(0)
              val name = s"$f $lastInitial."
              resourceBundle.getWithParams("shoutout.pushNotification.sentPhoto", Locale.ENGLISH, name, randomUnicode)

            }

            case (Some(f), None, _) => resourceBundle.getWithParams("shoutout.pushNotification.sentPhoto", Locale.ENGLISH, f, randomUnicode)
            case (None, Some(l), _) => resourceBundle.getWithParams("shoutout.pushNotification.sentPhoto", Locale.ENGLISH, l, randomUnicode)
            case (None, None, u)    => resourceBundle.getWithParams("shoutout.pushNotification.sentPhoto", Locale.ENGLISH, u, randomUnicode)
            case _                  => s"someone sent you a photo"
          }

          log.debug(s"Sending APNS notifications for recipients: $recipients")

          recipients.foreach {
            recipient =>

              log.debug(s"about to get the count")
              val unreadMessageCount = shoutoutDao.countUnread(recipient);

              log.debug(s"got the count now getting the tokens $unreadMessageCount")
              val tokens = sessionsDao.findTokensByUserId(recipient.id.get :: Nil)

              log.debug(s"using tokens: $tokens for user: $recipient")
              log.trace(s"sending notification with message $notificationMessage")

              tokens.getOrElse(recipient.id.get, Set.empty).foreach {
                token =>
                  val note = AppleNotification(
                    recipient.newMessagePush,
                    Some(token),
                    unreadMessageCount,
                    notificationMessage)
                  appleActor ! note
              }

          }

        }
      }

      /**
       * Admin only function - do not call this without an admin user
       */
      def sendToAll(sender : ShoutoutUser, url : String, text : Option[String], contentType : String, locale : Option[String]) : Int = {
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

      def sendToRecipients(sender : ShoutoutUser, url : String, text : Option[String], groupIds : Option[String], friendIds : Option[String], contentType : String) : Int = {

        var returnVal = 0

        db.withTransaction { implicit s : Session =>

          // find a unique list of shoutout users for each group provided
          val uniqueGroupIds : Set[Long] = groupIds match {
            case None     => Set()
            case Some(xs) => xs.split(',').toList.map(x => x.toLong).toSet
          }

          //remove all the groups that you do not own
          val (uniqueOwnedGroupIds, uniqueRejectedGroupIds) = uniqueGroupIds partition { g =>
            groupDao.isOwnerOfGroup(g, sender.id.get)
          }
          uniqueRejectedGroupIds foreach (item => log.error(s"User tried to send to a group they are not an owner of: $item"))

          val groupMembers = groupDao.findMembersForGroups(uniqueOwnedGroupIds)

          // find a unique list of shoutout user for each user id provided
          val uniqueFriendIds : Set[Long] = friendIds match {
            case None     => Set()
            case Some(xs) => xs.split(',').toList.map(x => x.toLong).toSet
          }
          val providedUsers = shoutoutUsersDao.findByIds(uniqueFriendIds)

          // for each of the users make sure we don't have any duplicates
          val recipients = providedUsers ::: groupMembers

          // find the blocked recipients
          val usersWhoBlockSender = blockUserDao.findUsersWhoBlockSender(recipients.toSet, sender)

          // unblocked recipients are the diff
          val uniqueRecipients = recipients.toSet.diff(usersWhoBlockSender).toList

          /**
           * This really could be quite expensive to do, we may want to figure out
           * why we think this needs to happen rather than do this check
           */
          //filter collection based on isFriend and isGroupOfOwner
          //partition this out so we can see how often it is happening
          val (filteredUniqueRecipients, filteredUniqueNonAssociations) = uniqueRecipients.partition(u => {
            contactsDao.isFriendOfOwner(sender, u) || contactsDao.isInGroupOfOwner(sender, u)
          })
          filteredUniqueNonAssociations foreach (item => log.error(s"Non associated item found: $item"))

          // insert a record for each user into the Shoutout table that isn't blocked
          shoutoutDao.insertShoutouts(filteredUniqueRecipients, Shoutout(
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

          // insert a record for each blocked user into the Shoutout table
          shoutoutDao.insertShoutouts(usersWhoBlockSender.toList, Shoutout(
            None,
            sender.id.get,
            0,
            text.getOrElse(""),
            url,
            false,
            None,
            Dates.nowDT,
            true,
            contentType
          ))

          // only send notifications to the non-blocked users
          sendAPNSNotificationsToRecipients(sender, filteredUniqueRecipients)

          // return the number of shoutouts that were sent (inserted)
          filteredUniqueRecipients.length
        }
      }

      def findAllForUser(user : ShoutoutUser, sessionId : UUID) : Future[List[ShoutoutResponse]] = {
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
                imageUrl = encryptField(resp.imageUrl),
                sender = resp.sender.copy(
                  facebookId = encryptOption(resp.sender.facebookId),
                  profilePictureUrl = encryptOption(resp.sender.profilePictureUrl)
                )
              )
            }
          }
        }
      }

      def updateShoutoutAsViewedForUser(user : ShoutoutUser, id : Long) : Future[Int] = {

        future {
          db.withTransaction { implicit s : Session =>
            shoutoutDao.setViewed(user, id)
          }
        }

      }
    }
}
