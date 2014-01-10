package com.phantom.ds.dataAccess

import org.specs2.mutable._
import com.phantom.dataAccess.DatabaseSupport
import com.phantom.model.Conversation
import org.specs2.specification.BeforeAfter

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 1/9/14
 * Time: 8:23 AM
 * To change this template use File | Settings | File Templates.
 */
class ConversationDAOSpec extends Specification with DatabaseSupport {

  object withSetupTeardown extends BeforeAfter {
    def before {
      println("Executing before stuff...")
      conversations.dropDB
      conversations.createDB

    }

    def after {
      println("Executing after astuff...")
      conversations.dropDB
    }
  }

  sequential

  "ConversationDAO" should {
    "support inserting users" in {

      withSetupTeardown.before

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

    "support searching for a conversation by owner id" in {
      withSetupTeardown.before
      conversations.insert(new Conversation(None, 3, 2))
      conversations.insert(new Conversation(None, 3, 4))
      val c = conversations.findByFromUserId(2)
      val c1 = conversations.findByFromUserId(4)
      (c(0).fromUser must equalTo(2)) and (c1(0).fromUser must equalTo(4))
    }

    "support deleting users" in {
      withSetupTeardown.before
      conversations.insert(new Conversation(None, 1, 2))
      conversations.insert(new Conversation(None, 3, 4))
      conversations.insert(new Conversation(None, 5, 6))
      (conversations.deleteById(1) must equalTo(1)) and
        (conversations.deleteById(2) must equalTo(1)) and
        (conversations.deleteById(3) must equalTo(1)) and
        (conversations.deleteById(4) must equalTo(0))
    }

    "support finding one conversation by id" in {
      withSetupTeardown.before
      conversations.insert(new Conversation(None, 1, 2))
      conversations.insert(new Conversation(None, 3, 4))
      conversations.insert(new Conversation(None, 5, 6))

      (conversations.findById(1).id.get must equalTo(1)) and
        (conversations.findById(1).toUser must equalTo(1)) and
        (conversations.findById(1).fromUser must equalTo(2))
    }

    "support inserting then updating a row" in {
      withSetupTeardown.before

      val inserted : Conversation = conversations.insert(new Conversation(None, 1, 2))

      val numRowsAffected : Int = conversations.updateById(new Conversation(inserted.id, 4, 5))

      val insertedFromDb : Conversation = conversations.findById(inserted.id.get)

      (numRowsAffected must equalTo(1)) and
        (insertedFromDb.id.get must equalTo(1)) and
        (insertedFromDb.fromUser must equalTo(5)) and
        (insertedFromDb.toUser must equalTo(4))
    }

  }

}
