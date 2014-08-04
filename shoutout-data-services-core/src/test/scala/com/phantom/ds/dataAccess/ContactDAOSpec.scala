package com.phantom.ds.dataAccess

import com.phantom.model.{ Contact }
import scala.slick.session.Session

class ContactDAOSpec extends BaseDAOSpec {

  sequential

  "ContactDAO" should {
    "Dumbest assertion possible" in withSetupTeardown {

      true should be equalTo true

    }

  }
}
