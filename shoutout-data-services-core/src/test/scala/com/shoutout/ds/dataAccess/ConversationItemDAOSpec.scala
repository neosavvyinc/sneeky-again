package com.shoutout.ds.dataAccess

import com.shoutout.model.ConversationItem
import com.shoutout.ds.TestUtils
import scala.concurrent.ExecutionContext.Implicits.global
import scala.slick.session.Session

class ConversationItemDAOSpec extends BaseDAOSpec with TestUtils {

  sequential

  "ConversationItemDAO" should {
    "Dumbest assertion possible" in withSetupTeardown {

      true should be equalTo true

    }

  }

}
