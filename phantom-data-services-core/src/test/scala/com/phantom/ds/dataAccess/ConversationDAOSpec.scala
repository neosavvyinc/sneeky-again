package com.phantom.ds.dataAccess

import com.phantom.model.Conversation
import com.phantom.ds.TestUtils
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 1/9/14
 * Time: 8:23 AM
 * To change this template use File | Settings | File Templates.
 */
class ConversationDAOSpec extends BaseDAOSpec with TestUtils {

  sequential

  "ConversationDAO" should {
    "support inserting conversations for users" in withSetupTeardown {

      insertTestUsers()

      val conv1 = conversationDao.insert(new Conversation(
        None, 1, 2, "9197419597"
      ))

      val conv2 = conversationDao.insert(new Conversation(
        None, 2, 3, "9197419598"
      ))

      conv2.id.get must equalTo(2)

    }

    "support searching for a conversation by owner id" in withSetupTeardown {
      insertTestUsers()

      conversationDao.insert(new Conversation(None, 3, 2, "9197419597"))
      conversationDao.insert(new Conversation(None, 3, 4, "9197419598"))
      val c = conversationDao.findByFromUserId(2)
      val c1 = conversationDao.findByFromUserId(4)
      (c(0).fromUser must equalTo(2)) and (c1(0).fromUser must equalTo(4))
    }

    "support finding one conversation by id" in withSetupTeardown {
      insertTestUsers()

      //dirty hack
      var dtTime = DateTime.now()
      dtTime = dtTime.minusMillis(dtTime.getMillisOfDay())

      conversationDao.insert(new Conversation(None, 1, 2, "9197419597", dtTime))
      conversationDao.insert(new Conversation(None, 3, 4, "9197419597", dtTime))
      conversationDao.insert(new Conversation(None, 5, 6, "9197419597", dtTime))

      (await(conversationDao.findById(1)).id.get must equalTo(1)) and
        (await(conversationDao.findById(1)).toUser must equalTo(1)) and
        (await(conversationDao.findById(1)).fromUser must equalTo(2))

      val c = await(conversationDao.findById(1))
      c must be_==(Conversation(Some(1), 1, 2, "9197419597", dtTime))
    }

    "support inserting then updating a row" in withSetupTeardown {
      insertTestUsers()

      //dirty hack
      var dtTime = DateTime.now()
      dtTime = dtTime.minusMillis(dtTime.getMillisOfDay)

      val inserted : Conversation = conversationDao.insert(new Conversation(None, 1, 2, "9197419597", dtTime))

      val numRowsAffected : Int = conversationDao.updateById(new Conversation(inserted.id, 4, 5, "9197419597", dtTime))
      numRowsAffected must equalTo(1)

      val insertedFromDb = conversationDao.findById(inserted.id.get)
      insertedFromDb must be_==(Conversation(Some(1), 4, 5, "9197419597", dtTime)).await

    }
  }

}
