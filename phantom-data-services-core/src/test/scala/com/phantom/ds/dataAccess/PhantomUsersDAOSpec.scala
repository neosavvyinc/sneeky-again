
package com.phantom.ds.dataAccess

import com.phantom.model.{ SoundOnNewNotification, PhantomUser }

class PhantomUsersDAOSpec extends BaseDAOSpec {

  sequential

  "PhantomUserDAO" should {

    "support finding a users contacts by phone number" in withSetupTeardown {

      insertUsersWithPhoneNumbersAndContacts

      val res = phantomUsersDao.findPhantomUserIdsByPhone(List("5192050", "2061266"))

      res._1.length must be equalTo (2)
      res._2.length must be equalTo (0)
    }

    "return a tuple of ids and non-found phone numbers" in withSetupTeardown {

      insertUsersWithPhoneNumbersAndContacts

      val res = phantomUsersDao.findPhantomUserIdsByPhone(List("5192050", "2061266", "7777777"))

      res._1.length must be equalTo (2)
      res._2.length must be equalTo (1)
    }

    "support updating an existing user's sound notification settings" in withSetupTeardown {

      val user : PhantomUser = createVerifiedUser("deadmau5@wobblebass.cx", "mouseears", "6665554455")

      val result = phantomUsersDao.updateSetting(user, SoundOnNewNotification, true)

      result must be equalTo (true)
    }

  }
}
