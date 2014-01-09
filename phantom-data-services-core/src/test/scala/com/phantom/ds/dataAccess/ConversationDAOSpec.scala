package com.phantom.ds.dataAccess

import org.specs2.mutable._
import com.phantom.dataAccess.DatabaseSupport
import com.phantom.model.Conversation

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 1/9/14
 * Time: 8:23 AM
 * To change this template use File | Settings | File Templates.
 */
class ConversationDAOSpec extends Specification with DatabaseSupport {

  sequential

  "ConversationDAO" should {
    "support inserting users" in {
      conversations.dropDB
      conversations.createDB

      conversations.insert(new Conversation(
        None, 1, 2
      )) must equalTo(1)
    }
    "support searching for a conversation by owner id" in {
      val c = conversations.findByOwnerId(1)
      c(0).fromUser must equalTo(2)
    }
    "support deleting users" in {
      conversations.deleteById(1) must equalTo(1)
    }

  }

}
