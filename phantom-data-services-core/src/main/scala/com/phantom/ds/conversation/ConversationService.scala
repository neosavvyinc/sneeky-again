package com.phantom.ds.conversation

import scala.concurrent.{ Future, ExecutionContext }
import com.phantom.model._
import scala.collection.mutable.{ Map => MMap }
import com.phantom.dataAccess.DatabaseSupport
import scala.slick.session.Session
import java.io.{ File, FileOutputStream }
import com.phantom.ds.DSConfiguration
import com.phantom.model.BlockUserByConversationResponse
import com.phantom.model.ConversationUpdateResponse
import com.phantom.model.Conversation
import com.phantom.model.ConversationItem
import com.phantom.model.ConversationInsertResponse
import com.phantom.ds.framework.exception.PhantomException

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 2:01 PM
 */
trait ConversationService {

  def findFeed(userId : Long) : Future[List[(Conversation, List[ConversationItem])]]

  def startConversation(fromUserId : Long,
                        toUserIds : List[Long],
                        imageText : String,
                        imageUrl : String) : Future[ConversationInsertResponse]

  def respondToConversation(conversationId : Long,
                            imageText : String,
                            imageUrl : String) : Future[ConversationUpdateResponse]

  def saveFileForConversationId(image : Array[Byte], conversationId : Long) : String

  def blockByConversationId(id : Long) : Future[BlockUserByConversationResponse]

}

object ConversationService extends DSConfiguration {

  def apply()(implicit ec : ExecutionContext) = new ConversationService with DatabaseSupport {
    def findFeed(userId : Long) : Future[List[(Conversation, List[ConversationItem])]] = {

      val returnValue : List[(Conversation, List[ConversationItem])] =
        conversations.findConversationsAndItems(userId)
      Future.successful(returnValue)
    }

    def startConversation(fromUserId : Long,
                          toUserIds : List[Long],
                          imageText : String,
                          imageUrl : String) : Future[ConversationInsertResponse] = {

      val session : Session = db.createSession
      var count = 0

      session.withTransaction {
        val startedConversations : List[Conversation] = for (toUserId <- toUserIds) yield Conversation(None, toUserId, fromUserId)
        val conversationsFromDB : List[Conversation] = startedConversations.map {
          conversation => conversations.insert(conversation)
        }

        conversationsFromDB.foreach {
          conversation => conversationItems.insert(ConversationItem(None, conversation.id.get, imageUrl, imageText))
        }

        count = startedConversations.size
      }

      session.close()

      Future.successful(ConversationInsertResponse(count))
    }

    def respondToConversation(conversationId : Long,
                              imageText : String,
                              imageUrl : String) : Future[ConversationUpdateResponse] = {

      val session : Session = db.createSession
      session.withTransaction {

        conversationItems.insert(ConversationItem(None, conversationId, imageUrl, imageText))

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

    def blockByConversationId(id : Long) : Future[BlockUserByConversationResponse] = {

      // find the base conversation
      val conversation = conversations.findById(id)

      // find the user that is being blocked - this should be the conv.toUser
      val userToBlock = phantomUsers.find(conversation.toUser)

      // see if the contact table has a reference in it for this association
      val contactToBlock : Option[Contact] = contacts.findByContactId(conversation.toUser, conversation.fromUser)

      // TODO: if not then insert a new record with type = block
      // else update the existing record with type = block

      val numUpdatedContacts = contactToBlock match {
        case Some(c : Contact) => contacts.update(c.copy(contactType = "block"))
        case _                 => -1
      }

      println("Num Updated Contacts: " + numUpdatedContacts)

      if (numUpdatedContacts == 1) {
        Future.successful(BlockUserByConversationResponse(contactToBlock.get.id.get, true))
      } else {
        Future.failed(throw PhantomException.contactNotUpdated)
      }

    }
  }

}