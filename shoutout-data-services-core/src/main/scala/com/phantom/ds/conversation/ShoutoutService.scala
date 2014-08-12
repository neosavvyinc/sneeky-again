package com.phantom.ds.conversation

import scala.concurrent.{ Future, ExecutionContext, future }
import com.phantom.model._
import com.phantom.dataAccess.DatabaseSupport
import com.phantom.ds.{ BasicCrypto, DSConfiguration }
import com.phantom.model.BlockUserByConversationResponse
import com.phantom.model.ConversationUpdateResponse
import com.phantom.model.Conversation
import com.phantom.model.ConversationItem
import com.phantom.model.ConversationInsertResponse
import com.phantom.ds.framework.{ Dates, Logging }
import akka.actor.ActorRef
import com.phantom.ds.integration.apple.AppleNotification
import com.phantom.ds.framework.exception.ShoutoutException
import scala.slick.session.Session
import java.util.UUID

import com.phantom.ds.integration.amazon.S3Service

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
          recipients.foreach(println)
          val uniqueRecipients = recipients.toSet.toList

          // insert a record for each user into the Shoutout table
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
