package com.phantom.ds.dataAccess

import org.specs2.specification.BeforeAfter
import org.specs2.mutable.Specification
import com.phantom.dataAccess.DatabaseSupport
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

      val ret = conversationItems.insert(item)

      (ret.id.get must equalTo(1)) and
        (ret.conversationId must equalTo(1)) and
        (ret.imageUrl must equalTo("imageUrl")) and
        (ret.imageText must equalTo("imageText"))

    }

    "support inserting a collection of conversation items and finding them by conversation id" in withSetupTeardown {

      insertTestUsersAndConversations

      val list = setupConversationItems(1)

      conversationItems.insertAll(list)

      val itemsFromDb = conversationItems.findByConversationId(1)

      itemsFromDb.length must equalTo(3)

    }

    "support inserting 3 records for one conv then 3 for another then delete by conv id" in withSetupTeardown {

      insertTestUsersAndConversations

      val conv1 = setupConversationItems(1)
      val conv2 = setupConversationItems(2)
      conversationItems.insertAll(conv1)
      conversationItems.insertAll(conv2)

      val conv1FromDB = conversationItems.findByConversationId(1)
      val conv2FromDB = conversationItems.findByConversationId(2)

      conversationItems.deleteByConversationId(1)

      val conv1FromDBAfterDelete = conversationItems.findByConversationId(1)
      val conv2FromDBAfterDelete = conversationItems.findByConversationId(2)

      (conv1FromDBAfterDelete.length must equalTo(0)) and
        (conv2FromDBAfterDelete.length must equalTo(3))

    }

  }

}
