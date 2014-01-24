
package com.phantom.ds.dataAccess

import org.specs2.mutable._
import com.phantom.dataAccess.DatabaseSupport
import com.phantom.model.{ PhantomUser }
import org.specs2.specification.BeforeAfter

class PhantomDAOSpec extends BaseDAOSpec {

  sequential

  "PhantomUserDAO" should {

    "support finding a users contacts by phone number" in withSetupTeardown {

      insertUsersWithPhoneNumbersAndContacts

      val res = phantomUsers.findPhantomUserIdsByPhone(List("5192050", "2061266"))

      res must be_==(List[Long](2, 3)).await
    }
  }
}
