package com.phantom.ds.dataAccess

import com.phantom.model.Conversation

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

      val conv1 = conversationDao.insert(new Conversation(
        None, 1, 2
      ))
      println(conv1)
      val conv2 = conversationDao.insert(new Conversation(
        None, 2, 3
      ))
      println(conv2)
      conv2.id.get must equalTo(2)

    }

    "support searching for a conversation by owner id" in withSetupTeardown {
      insertTestUsers

      conversationDao.insert(new Conversation(None, 3, 2))
      conversationDao.insert(new Conversation(None, 3, 4))
      val c = conversationDao.findByFromUserId(2)
      val c1 = conversationDao.findByFromUserId(4)
      (c(0).fromUser must equalTo(2)) and (c1(0).fromUser must equalTo(4))
    }

    "support deleting conversations" in withSetupTeardown {
      insertTestUsers

      conversationDao.insert(new Conversation(None, 1, 2))
      conversationDao.insert(new Conversation(None, 3, 4))
      conversationDao.insert(new Conversation(None, 5, 6))
      (conversationDao.deleteById(1) must equalTo(1)) and
        (conversationDao.deleteById(2) must equalTo(1)) and
        (conversationDao.deleteById(3) must equalTo(1)) and
        (conversationDao.deleteById(4) must equalTo(0))
    }

    "support finding one conversation by id" in withSetupTeardown {
      insertTestUsers

      conversationDao.insert(new Conversation(None, 1, 2))
      conversationDao.insert(new Conversation(None, 3, 4))
      conversationDao.insert(new Conversation(None, 5, 6))

      (conversationDao.findById(1).id.get must equalTo(1)) and
        (conversationDao.findById(1).toUser must equalTo(1)) and
        (conversationDao.findById(1).fromUser must equalTo(2))
      conversations.findById(1) must be_==(Conversation(Some(1), 1, 2)).await
    }

    "support inserting then updating a row" in withSetupTeardown {
      insertTestUsers

      val inserted : Conversation = conversationDao.insert(new Conversation(None, 1, 2))

      val numRowsAffected : Int = conversations.updateById(new Conversation(inserted.id, 4, 5))
      numRowsAffected must equalTo(1)

      val insertedFromDb = conversations.findById(inserted.id.get)
      insertedFromDb must be_==(Conversation(Some(1), 4, 5)).await

    }
  }

}
