package com.shoutout.ds.dataAccess

import com.shoutout.model.Conversation
import com.shoutout.ds.TestUtils
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
    "Dumbest assertion possible" in withSetupTeardown {

      true should be equalTo true

    }

  }

}
