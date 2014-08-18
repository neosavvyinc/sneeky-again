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

  def saveImage(image : Array[Byte]) : Future[String]
  def sendToRecipients(sender : ShoutoutUser, url : String, imageText : Option[String], groupIds : Option[String], friendIds : Option[String]) : Int
  def findAllForUser(user : ShoutoutUser) : Future[List[ShoutoutResponse]]
  def updateShoutoutAsViewedForUser(user : ShoutoutUser, id : Long) : Future[Int]

}

object ShoutoutService extends DSConfiguration with BasicCrypto {

  def apply(appleActor : ActorRef, s3Service : S3Service)(implicit ec : ExecutionContext) =
    new ShoutoutService with DatabaseSupport with Logging {

      def saveImage(image : Array[Byte]) : Future[String] = {
        s3Service.saveImage(image)
      }

      private def sendAPNSNotificationsToRecipients(sender : ShoutoutUser, recipients : List[ShoutoutUser])(implicit session : Session) : Future[Unit] = {
        future {

          val notificationMessage = (sender.firstName, sender.lastName, sender.username) match {
            case (Some(f), Some(l), _) => {
              val lastInitial = l.charAt(0)
              s"$f $lastInitial. has sent you a photo"
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

      def sendToRecipients(sender : ShoutoutUser, url : String, imageText : Option[String], groupIds : Option[String], friendIds : Option[String]) : Int = {

        db.withTransaction { implicit s : Session =>

          // find a unique list of shoutout users for each group provided
          val uniqueGroupIds : Set[Long] = groupIds match {
            case None     => Set()
            case Some(xs) => xs.split(',').toList.map(x => x.toLong).toSet
          }
          val groupMembers = groupDao.findMembersForGroups(uniqueGroupIds)

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

          // insert a record for each user into the Shoutout table that isn't blocked
          shoutoutDao.insertShoutouts(sender, uniqueRecipients, Shoutout(
            None,
            sender.id.get,
            0,
            imageText.getOrElse(""),
            url,
            false,
            None,
            Dates.nowDT
          ))

          // insert a record for each blocked user into the Shoutout table
          shoutoutDao.insertShoutouts(sender, usersWhoBlockSender.toList, Shoutout(
            None,
            sender.id.get,
            0,
            imageText.getOrElse(""),
            url,
            false,
            None,
            Dates.nowDT,
            true
          ))

          // only send notifications to the non-blocked users
          sendAPNSNotificationsToRecipients(sender, uniqueRecipients)

          // return the number of shoutouts that were sent (inserted)
          uniqueRecipients.length
        }
      }

      def findAllForUser(user : ShoutoutUser) : Future[List[ShoutoutResponse]] = {
        future {
          db.withSession { implicit s : Session =>
            shoutoutDao.findAllForUser(user)
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
