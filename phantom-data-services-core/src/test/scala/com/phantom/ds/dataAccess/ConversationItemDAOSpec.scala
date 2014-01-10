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
    "support inserting one conversation item" in {

      val item = new ConversationItem(
        None, 1, "imageUrl", "imageText"
      )

      val ret = conversationItems.insert(item)

      (ret.id.get must equalTo(1)) and
        (ret.conversationId must equalTo(1)) and
        (ret.imageUrl must equalTo("imageUrl")) and
        (ret.imageText must equalTo("imageText"))

    }
  }

}
