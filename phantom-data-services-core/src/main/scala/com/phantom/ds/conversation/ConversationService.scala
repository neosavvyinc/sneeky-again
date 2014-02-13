package com.phantom.ds.conversation

import scala.concurrent.{ Future, ExecutionContext, future }
import com.phantom.model._
import com.phantom.dataAccess.DatabaseSupport
import java.io.{ File, FileOutputStream }
import com.phantom.ds.DSConfiguration
import com.phantom.model.BlockUserByConversationResponse
import com.phantom.model.ConversationUpdateResponse
import com.phantom.model.Conversation
import com.phantom.model.ConversationItem
import com.phantom.model.ConversationInsertResponse
import com.phantom.ds.framework.Logging
import akka.actor.ActorRef
import com.phantom.ds.integration.twilio.SendInviteToStubUsers
import com.phantom.ds.framework.exception.PhantomException
import scala.slick.session.Session
import java.util.UUID

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 2:01 PM
 */
trait ConversationService {

  def findFeed(userId : Long) : Future[List[FeedEntry]]

  def startConversation(fromUserId : Long,
                        contactNumbers : Set[String],
                        imageText : String,
                        imageUrl : String) : Future[ConversationInsertResponse]

  def respondToConversation(userId : Long,
                            conversationId : Long,
                            imageText : String,
                            image : Array[Byte]) : Future[ConversationUpdateResponse]

  def saveFileForConversationId(image : Array[Byte], conversationId : Long) : String

  def blockByConversationId(userId : Long, conversationId : Long) : Future[BlockUserByConversationResponse]

}

object ConversationService extends DSConfiguration {

  def apply(twilioActor : ActorRef, appleActor : ActorRef)(implicit ec : ExecutionContext) = new ConversationService with DatabaseSupport with Logging {

    def findFeed(userId : Long) : Future[List[FeedEntry]] = {
      conversationDao.findConversationsAndItems(userId)
    }

    def startConversation(fromUserId : Long,
                          contactNumbers : Set[String],
                          imageText : String,
                          imageUrl : String) : Future[ConversationInsertResponse] = {
      for {
        (nonUsers, allUsers) <- partitionUsers(contactNumbers)
        (stubUsers, users) <- partitionStubUsers(allUsers)
        (newStubUsers, response) <- createStubUsersAndRoots(nonUsers, users ++ stubUsers, fromUserId, imageText, imageUrl)
        _ <- sendInvitations(stubUsers ++ newStubUsers)
        tokens <- getTokens(allUsers.map(_.id.get))
        _ <- sendConversationNotifications(tokens)
      } yield response
    }

    private def getTokens(userIds : Seq[Long]) : Future[List[String]] = {
      future {
        sessions.findTokensByUserId(userIds)
      }
    }

    private def partitionUsers(contactNumbers : Set[String]) : Future[(Set[String], Seq[PhantomUser])] = {
      partition(phantomUsersDao.findByPhoneNumbers(contactNumbers), contactNumbers)
    }

    private def partitionStubUsers(users : Seq[PhantomUser]) : Future[(Seq[PhantomUser], Seq[PhantomUser])] = {
      Future.successful(users.partition(_.status == Stub))
    }

    private def partition(phantomsF : Future[List[PhantomUser]], contactNumbers : Set[String]) : Future[(Set[String], Seq[PhantomUser])] = {
      phantomsF.map { users =>
        val existingNumbers = users.map(_.phoneNumber).flatten.toSet
        val nonUsers = contactNumbers.diff(existingNumbers)
        (nonUsers, users)
      }
    }

    private def createStubUsersAndRoots(numbers : Set[String], users : Seq[PhantomUser], fromUserId : Long, imageText : String, imageUrl : String) : Future[(Seq[PhantomUser], ConversationInsertResponse)] = {
      future {
        db.withTransaction { implicit session : Session =>
          val newUsers = phantomUsersDao.insertAllOperation(numbers.map(x => PhantomUser(None, UUID.randomUUID, None, None, None, false, Some(x), Stub, 0)).toSeq)
          val conversations = (users ++ newUsers).map(x => Conversation(None, x.id.getOrElse(-1), fromUserId, x.phoneNumber.get))
          val createdConversations = conversationDao.insertAllOperation(conversations)
          conversationItemDao.insertAllOperation(createConversationItemRoots(createdConversations, fromUserId, imageText, imageUrl))
          (newUsers, ConversationInsertResponse(createdConversations.size))
        }
      }
    }

    private def createConversationItemRoots(conversations : Seq[Conversation], fromUserId : Long, imageText : String, imageUrl : String) : Seq[ConversationItem] = {
      conversations.map(x => ConversationItem(None, x.id.getOrElse(-1), imageUrl, imageText, x.toUser, fromUserId))
    }

    private def sendConversationNotifications(pushTokens : List[String]) : Future[Unit] = {
      future {
        pushTokens.foreach(appleActor ! _)
      }
    }

    //TODO: FIRE A MESSAGE PER USER, NOT PER BATCH
    private def sendInvitations(stubUsers : Seq[PhantomUser]) : Future[Unit] = {
      //intentionally not creating a future here..as sending msgs in non blocking
      Future.successful {
        val invitable = stubUsers.filter(_.invitationCount < UserConfiguration.invitationMax)
        if (!invitable.isEmpty) {
          twilioActor ! SendInviteToStubUsers(invitable)
        }
      }
    }

    def findToUser(toUser : Long, conversation : Conversation) : Long = {
      if (conversation.toUser == toUser) {
        toUser
      } else {
        conversation.fromUser
      }
    }

    def findFromUser(toUser : Long, conversation : Conversation) : Long = {
      if (conversation.toUser == toUser) {
        conversation.fromUser
      } else {
        toUser
      }
    }

    override def respondToConversation(userId : Long, conversationId : Long, imageText : String, image : Array[Byte]) : Future[ConversationUpdateResponse] = {
      future {
        db.withTransaction { implicit session =>
          val citem = for {
            conversation <- conversationDao.findByIdAndUserOperation(conversationId, userId)
          } yield conversationItemDao.insertOperation(
            ConversationItem(
              None,
              conversationId,
              saveFileForConversationId(
                image,
                conversationId),
              imageText,
              findToUser(
                userId,
                conversation),
              findFromUser(
                userId,
                conversation)
            )
          )
          citem.map(x => ConversationUpdateResponse(1)).getOrElse(throw PhantomException.nonExistentConversation)
        }
      }
    }

    def saveFileForConversationId(image : Array[Byte], conversationId : Long) : String = {

      val imageDir = FileStoreConfiguration.baseDirectory + conversationId
      val imageUrl = imageDir + "/image"
      val dir : File = new File(imageDir)
      if (!dir.exists())
        dir.mkdirs()

      val fos : FileOutputStream = new FileOutputStream(imageUrl)

      try {
        fos.write(image)
      } finally {
        fos.close()
      }

      imageUrl

    }

    def blockByConversationId(userId : Long, conversationId : Long) : Future[BlockUserByConversationResponse] = {
      future {
        db.withTransaction { implicit session =>

          val updatedOpt = for {
            conversation <- conversationDao.findByIdAndUserOperation(conversationId, userId)
            updateCount <- Option(contacts.blockContactOperation(userId, getOtherUserId(conversation, userId)))
          } yield (updateCount, conversation)

          updatedOpt match {
            case None => throw PhantomException.nonExistentConversation
            case Some((0, conversation)) =>
              backfillBlockedContact(userId, getOtherUserId(conversation, userId)); BlockUserByConversationResponse(conversation.id.get, true)
            case Some((_, conversation)) => BlockUserByConversationResponse(conversation.id.get, true)
          }
        }
      }
    }

    def viewConversationItem(conversationItemId : Long, userId : Long) : Future[Boolean] = {

      future {
        db.withTransaction { implicit session =>
          conversationItemDao.updateViewed(conversationItemId, userId) > 0
        }
      }

    }

    private def backfillBlockedContact(ownerId : Long, contactId : Long)(implicit session : Session) : Contact = {
      contacts.insertOperation(Contact(None, ownerId, contactId, Blocked))
    }

    //no check to see if the userId is present in here..only called by the function above
    private def getOtherUserId(conversation : Conversation, userId : Long) = {
      if (conversation.fromUser == userId) {
        conversation.toUser
      } else {
        conversation.fromUser
      }
    }
  }
}
