package com.phantom.ds.conversation

import scala.concurrent.{ Future, ExecutionContext, future }
import com.phantom.model._
import com.phantom.dataAccess.DatabaseSupport
import scala.slick.session.Session
import java.io.{ File, FileOutputStream }
import com.phantom.ds.DSConfiguration
import com.phantom.model.ConversationUpdateResponse
import com.phantom.model.Conversation
import com.phantom.model.ConversationItem
import com.phantom.model.ConversationInsertResponse
import com.phantom.ds.framework.Logging

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

}

object ConversationService extends DSConfiguration {

  def apply()(implicit ec : ExecutionContext) = new ConversationService with DatabaseSupport with Logging {

    def findFeed(userId : Long) : Future[List[(Conversation, List[ConversationItem])]] = {
      future {
        conversationDao.findConversationsAndItems(userId)
      }
    }

    def startConversation(fromUserId : Long,
                          contactNumbers : Set[String],
                          imageText : String,
                          imageUrl : String) : Future[ConversationInsertResponse] = {
      for {
        (nonUsers, users) <- partitionUsers(contactNumbers)
        response <- createConversationRoots(users, fromUserId, imageText, imageUrl)
        _ <- sendInvitations(nonUsers)
        _ <- sendConversationNotifications(users)
      } yield response

    }

    //are there 3 segments here?  stub users ? fuck
    private def partitionUsers(contactNumbers : Set[String]) : Future[(Set[String], Seq[PhantomUser])] = {
      phantomUsers.findByPhoneNumbers(contactNumbers).map { users =>
        val existingNumbers = users.map(_.phoneNumber).toSet
        val nonUsers = contactNumbers.diff(existingNumbers)
        (nonUsers, users)
      }
    }

    private def createConversationRoots(users : Seq[PhantomUser], fromUserId : Long, imageText : String, imageUrl : String) : Future[ConversationInsertResponse] = {
      val conversations = users.map(x => Conversation(None, x.id.getOrElse(-1), fromUserId))
      val session : Session = db.createSession

      val b = session.withTransaction {
        for {
          createdConversations <- conversationDao.insertAll(conversations)
          createdConversationItems <- conversationItemDao.insertAll(createConversationItemRoots(createdConversations, fromUserId, imageText, imageUrl))
        } yield ConversationInsertResponse(createdConversations.size)
      }
      session.close()
      b
    }

    private def createConversationItemRoots(conversations : Seq[Conversation], fromUserId : Long, imageText : String, imageUrl : String) : Seq[ConversationItem] = {
      conversations.map(x => ConversationItem(None, x.id.getOrElse(-1), imageUrl, imageText))
    }

    private def sendConversationNotifications(phantomUsers : Seq[PhantomUser]) : Future[Unit] = {
      Future.successful()
    }

    private def sendInvitations(contacts : Set[String]) : Future[Unit] = {
      Future.successful()
    }

    def respondToConversation(conversationId : Long,
                              imageText : String,
                              imageUrl : String) : Future[ConversationUpdateResponse] = {

      val session : Session = db.createSession
      session.withTransaction {

        conversationItemDao.insert(ConversationItem(None, conversationId, imageUrl, imageText))

        Future.successful(ConversationUpdateResponse(1))
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
  }

}