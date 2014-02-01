package com.phantom.ds.dataAccess

import com.phantom.model.ConversationItem

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 1/9/14
 * Time: 8:39 PM
 * To change this template use File | Settings | File Templates.
 */
class ConversationItemDAOSpec extends BaseDAOSpec {

  sequential

  "ConversationItemDAO" should {
    "support inserting one conversation item" in withSetupTeardown {

      insertTestUsersAndConversations

      val item = new ConversationItem(
        None, 1, "imageUrl", "imageText"
      )

      val ret = conversationItemDao.insert(item)

      (ret.id.get must equalTo(1)) and
        (ret.conversationId must equalTo(1)) and
        (ret.imageUrl must equalTo("imageUrl")) and
        (ret.imageText must equalTo("imageText"))

    }

    "support inserting a collection of conversation items and finding them by conversation id" in withSetupTeardown {

      insertTestUsersAndConversations

      val list = setupConversationItems(1)

      conversationItemDao.insertAll(list)

      val itemsFromDb = conversationItemDao.findByConversationId(1)

      itemsFromDb.length must equalTo(3)

    }

    "support inserting 3 records for one conv then 3 for another then delete by conv id" in withSetupTeardown {

      insertTestUsersAndConversations

      val conv1 = setupConversationItems(1)
      val conv2 = setupConversationItems(2)
      conversationItemDao.insertAll(conv1)
      conversationItemDao.insertAll(conv2)

      val conv1FromDB = conversationItemDao.findByConversationId(1)
      val conv2FromDB = conversationItemDao.findByConversationId(2)

      conversationItemDao.deleteByConversationId(1)

      val conv1FromDBAfterDelete = conversationItemDao.findByConversationId(1)
      val conv2FromDBAfterDelete = conversationItemDao.findByConversationId(2)

      (conv1FromDBAfterDelete.length must equalTo(0)) and
        (conv2FromDBAfterDelete.length must equalTo(3))

    }

  }

}
