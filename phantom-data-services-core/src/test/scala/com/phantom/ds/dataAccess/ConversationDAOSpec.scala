package com.phantom.ds.dataAccess

import org.specs2.mutable._
import com.phantom.dataAccess.DatabaseSupport
import com.phantom.model.{ ConversationItem, PhantomUser, Conversation }
import org.specs2.specification.BeforeAfter
import org.joda.time.LocalDate

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 1/9/14
 * Time: 8:23 AM
 * To change this template use File | Settings | File Templates.
 */
class ConversationDAOSpec extends BaseDAOSpec {

  sequential

  "ConversationDAO" should {
    "support inserting conversations for users" in withSetupTeardown {

      insertTestUsers

      val conv1 = conversations.insert(new Conversation(
        None, 1, 2
      ))
      println(conv1)
      val conv2 = conversations.insert(new Conversation(
        None, 2, 3
      ))
      println(conv2)
      conv2.id.get must equalTo(2)

    }

    "support searching for a conversation by owner id" in withSetupTeardown {
      insertTestUsers

      conversations.insert(new Conversation(None, 3, 2))
      conversations.insert(new Conversation(None, 3, 4))
      val c = conversations.findByFromUserId(2)
      val c1 = conversations.findByFromUserId(4)
      (c(0).fromUser must equalTo(2)) and (c1(0).fromUser must equalTo(4))
    }

    "support deleting conversations" in withSetupTeardown {
      insertTestUsers

      conversations.insert(new Conversation(None, 1, 2))
      conversations.insert(new Conversation(None, 3, 4))
      conversations.insert(new Conversation(None, 5, 6))
      (conversations.deleteById(1) must equalTo(1)) and
        (conversations.deleteById(2) must equalTo(1)) and
        (conversations.deleteById(3) must equalTo(1)) and
        (conversations.deleteById(4) must equalTo(0))
    }

    "support finding one conversation by id" in withSetupTeardown {
      insertTestUsers

      conversations.insert(new Conversation(None, 1, 2))
      conversations.insert(new Conversation(None, 3, 4))
      conversations.insert(new Conversation(None, 5, 6))

      (conversations.findById(1).id.get must equalTo(1)) and
        (conversations.findById(1).toUser must equalTo(1)) and
        (conversations.findById(1).fromUser must equalTo(2))
    }

    "support inserting then updating a row" in withSetupTeardown {
      insertTestUsers

      val inserted : Conversation = conversations.insert(new Conversation(None, 1, 2))

      val numRowsAffected : Int = conversations.updateById(new Conversation(inserted.id, 4, 5))

      val insertedFromDb : Conversation = conversations.findById(inserted.id.get)

      (numRowsAffected must equalTo(1)) and
        (insertedFromDb.id.get must equalTo(1)) and
        (insertedFromDb.fromUser must equalTo(5)) and
        (insertedFromDb.toUser must equalTo(4))
    }

    "support returning a list of conversations with a collection of their conversation items attached as a tuple" in withSetupTeardown {
      insertTestConverationsWithItems

      val feed : List[(Conversation, ConversationItem)] = conversations.findConversationsAndItems(1)

      feed.foreach {
        c =>
          val (conv, ci) = c
          println("ConvId: " + conv.id + " " + conv.toUser + " " + conv.fromUser + " " + ci.imageText + " " + ci.imageUrl)

        //          ci.foreach {
        //            cItem =>
        //              println(cItem.id + " " + cItem.imageText + " " + cItem.imageUrl)
        //          }

      }

      true must equalTo(false) //lacks a real assertion

    }.pendingUntilFixed("Adam will fix this")

  }

}
