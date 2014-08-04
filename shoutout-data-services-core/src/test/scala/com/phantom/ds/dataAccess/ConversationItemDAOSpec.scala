package com.phantom.ds.dataAccess

import com.phantom.model.ConversationItem
import com.phantom.ds.TestUtils
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
