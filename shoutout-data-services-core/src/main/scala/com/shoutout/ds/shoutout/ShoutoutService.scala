package com.shoutout.ds.shoutout

import scala.concurrent.{ Future, ExecutionContext, future }
import com.shoutout.model._
import com.shoutout.dataAccess.DatabaseSupport
import com.shoutout.ds.{ BasicCrypto, DSConfiguration }
import com.shoutout.model.BlockUserByConversationResponse
import com.shoutout.model.ConversationUpdateResponse
import com.shoutout.model.Conversation
import com.shoutout.model.ConversationItem
import com.shoutout.model.ConversationInsertResponse
import com.shoutout.ds.framework.{ Dates, Logging }
import akka.actor.ActorRef
import com.shoutout.ds.integration.apple.AppleNotification
import com.shoutout.ds.framework.exception.ShoutoutException
import scala.slick.session.Session
import java.util.UUID

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

      def saveData(data : Array[Byte], contentType : String) : Future[String] = {
        s3Service.saveData(data, contentType)
      }

      private def sendAPNSNotificationsToRecipients(sender : ShoutoutUser, recipients : List[ShoutoutUser])(implicit session : Session) : Future[Unit] = {
        future {

          val notificationMessage = (sender.firstName, sender.lastName, sender.username) match {
            case (Some(f), Some(l), _) => {
              val lastInitial = l.charAt(0)
              s"$f $lastInitial. sent you a photo"
            }
            case (Some(f), None, _) => s"$f sent you a photo"
            case (None, Some(l), _) => s"$l sent you a photo"
            case (None, None, u)    => s"$u sent you a photo"
            case _                  => s"someone sent you a photo"
          }

          recipients.foreach {
            recipient =>
              val unreadMessageCount = shoutoutDao.countUnread(recipient);
              val tokens = sessionsDao.findTokensByUserId(recipient.id.get :: Nil)
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
