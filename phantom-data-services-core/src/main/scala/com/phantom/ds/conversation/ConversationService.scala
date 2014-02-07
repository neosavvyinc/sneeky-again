package com.phantom.ds.conversation

import scala.concurrent.{ Promise, Future, ExecutionContext, future }
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
import com.phantom.ds.integration.twilio.{ SendInvite, SendInviteToStubUsers }
import com.phantom.ds.integration.apple.SendConversationNotification
import com.phantom.ds.framework.exception.PhantomException
import scala.util.{ Failure, Success }

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 2:01 PM
 */
trait ConversationService {

  //  * Get my feed
  //    * Request
  //    * UserId
  //    * Response
  //    * List of conversations
  def findFeed(userId : Long) : Future[List[(Conversation, List[ConversationItem])]]

  def startConversation(fromUserId : Long,
                        contactNumbers : Set[String],
                        imageText : String,
                        imageUrl : String) : Future[ConversationInsertResponse]

  def respondToConversation(conversationId : Long,
                            imageText : String,
                            imageUrl : String) : Future[ConversationUpdateResponse]

  def saveFileForConversationId(image : Array[Byte], conversationId : Long) : String

  def blockByConversationId(id : Long) : Future[BlockUserByConversationResponse]

}

object ConversationService extends DSConfiguration {

  def apply(twilioActor : ActorRef, appleActor : ActorRef)(implicit ec : ExecutionContext) = new ConversationService with DatabaseSupport with Logging {

    def findFeed(userId : Long) : Future[List[(Conversation, List[ConversationItem])]] = {
      conversationDao.findConversationsAndItems(userId)
    }

    def startConversation(fromUserId : Long,
                          contactNumbers : Set[String],
                          imageText : String,
                          imageUrl : String) : Future[ConversationInsertResponse] = {
      for {
        (nonUsers, users) <- partitionUsers(contactNumbers)
        (nonStubUsers, stubUsers) <- partitionStubUsers(nonUsers)
        response <- createConversationRoots(users, stubUsers, fromUserId, imageText, imageUrl)
        _ <- sendInvitations(nonStubUsers, stubUsers, fromUserId, imageText, imageUrl)
        _ <- sendConversationNotifications(users)
      } yield response
    }

    private def partitionUsers(contactNumbers : Set[String]) : Future[(Set[String], Seq[PhantomUser])] = {
      partition(phantomUsersDao.findByPhoneNumbers(contactNumbers), contactNumbers)
    }

    private def partitionStubUsers(contactNumbers : Set[String]) : Future[(Set[String], Seq[StubUser])] = {
      partition(stubUsersDao.findByPhoneNumbers(contactNumbers), contactNumbers)
    }

    private def partition[T <: Phantom](phantomsF : Future[List[T]], contactNumbers : Set[String]) : Future[(Set[String], Seq[T])] = {
      phantomsF.map { users =>
        val existingNumbers = users.map(_.phoneNumber).toSet
        val nonUsers = contactNumbers.diff(existingNumbers)
        (nonUsers, users)
      }
    }

    private def createConversationRoots(users : Seq[PhantomUser], stubUsers : Seq[StubUser], fromUserId : Long, imageText : String, imageUrl : String) : Future[ConversationInsertResponse] = {
      future {
        db.withTransaction { implicit session =>
          val conversations = users.map(x => Conversation(None, x.id.getOrElse(-1), fromUserId))
          val stubConversations = stubUsers.map(x => StubConversation(None, fromUserId, x.id.getOrElse(-1), imageText, imageUrl))
          val createdConversations = conversationDao.insertAllOperation(conversations)
          val createdConversationItems = conversationItemDao.insertAllOperation(createConversationItemRoots(createdConversations, fromUserId, imageText, imageUrl))
          val createdStubConversations = stubConversationsDao.insertAllOperation(stubConversations)
          ConversationInsertResponse(createdConversations.size + createdStubConversations.size)
        }
      }
    }

    private def createConversationItemRoots(conversations : Seq[Conversation], fromUserId : Long, imageText : String, imageUrl : String) : Seq[ConversationItem] = {
      conversations.map(x => ConversationItem(None, x.id.getOrElse(-1), imageUrl, imageText))
    }

    private def sendConversationNotifications(phantomUsers : Seq[PhantomUser]) : Future[Unit] = {

      Future.successful(if (!phantomUsers.isEmpty) { appleActor ! SendConversationNotification(phantomUsers) })
    }

    private def sendInvitations(contacts : Set[String], stubUsers : Seq[StubUser], fromUser : Long, imageText : String, imageUrl : String) : Future[Unit] = {
      //intentionally not creating a future here..as sending msgs in non blocking
      Future.successful {
        val invitable = stubUsers.filter(_.invitationCount < UserConfiguration.invitationMax)
        if (!invitable.isEmpty) {
          twilioActor ! SendInviteToStubUsers(invitable)
        }
        if (!contacts.isEmpty) {
          twilioActor ! SendInvite(contacts, fromUser, imageText, imageUrl)
        }
      }
    }

    def respondToConversation(conversationId : Long,
                              imageText : String,
                              imageUrl : String) : Future[ConversationUpdateResponse] = {
      conversationItemDao.insert(ConversationItem(None, conversationId, imageUrl, imageText)).map(x => ConversationUpdateResponse(1))
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

    //TODO: remove promise
    def blockByConversationId(id : Long) : Future[BlockUserByConversationResponse] = {

      val blockUserPromise : Promise[BlockUserByConversationResponse] = Promise()

      future {
        val contact = for {
          c <- conversationDao.findById(id)
          cb <- contacts.findByContactId(c.toUser, c.fromUser)
          n <- contacts.update(cb.copy(contactType = "block"))
        } yield cb

        contact.onComplete {
          case Success(c : Contact) => blockUserPromise.success(BlockUserByConversationResponse(c.id.get, true))
          case Failure(ex)          => blockUserPromise.failure(PhantomException.contactNotUpdated)
        }
      }

      blockUserPromise.future
    }
  }

}
