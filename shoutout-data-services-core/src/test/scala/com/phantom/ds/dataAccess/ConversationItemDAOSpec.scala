package com.phantom.ds.dataAccess

import com.phantom.model.ConversationItem
import com.phantom.ds.TestUtils
import scala.concurrent.ExecutionContext.Implicits.global
import scala.slick.session.Session

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 1/9/14
 * Time: 8:39 PM
 * To change this template use File | Settings | File Templates.
 */
class ConversationItemDAOSpec extends BaseDAOSpec with TestUtils {

  sequential

  "ConversationItemDAO" should {
    "support inserting one conversation item" in withSetupTeardown {

      insertTestUsersAndConversations()

      val item = new ConversationItem(
        None, 1, "imageUrl", "imageText", 1, 2
      )
      val ret = db.withTransaction { implicit session : Session => conversationItemDao.insertOperation(item) }

      (ret.id.get must equalTo(1)) and
        (ret.conversationId must equalTo(1)) and
        (ret.imageUrl must equalTo("imageUrl")) and
        (ret.imageText must equalTo("imageText"))

    }

    "support inserting a collection of conversation items and finding them by conversation id" in withSetupTeardown {

      insertTestUsersAndConversations()

      val list = setupConversationItems(1, 1, 2)

      await {
        conversationItemDao.insertAll(list)
      }

      val itemsFromDb = db.withSession { implicit session : Session =>
        conversationItemDao.findByConversationIdAndUserOperation(1, 1)
      }
      itemsFromDb.length must equalTo(3)

    }

  }

}
