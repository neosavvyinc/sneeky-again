
package com.phantom.ds.dataAccess

import com.phantom.model.{ NotificationOnNewPicture, SoundOnNewNotification, PhantomUser }

class PhantomUsersDAOSpec extends BaseDAOSpec {

  sequential

  "PhantomUserDAO" should {

    "support finding a users contacts by phone number" in withSetupTeardown {

      insertUsersWithPhoneNumbersAndContacts()

      val res = phantomUsersDao.findPhantomUserIdsByPhone(List("111111", "222222"))

      res._1.length must be equalTo 2
      res._2.length must be equalTo 0
    }

    "return a tuple of ids and non-found phone numbers" in withSetupTeardown {

      insertUsersWithPhoneNumbersAndContacts()

      val res = phantomUsersDao.findPhantomUserIdsByPhone(List("111111", "222222", "0909090"))

      res._1.length must be equalTo 2
      res._2.length must be equalTo 1
    }

    "support updating an existing user's sound notification settings" in withSetupTeardown {

      val user : PhantomUser = createVerifiedUser("deadmau5@wobblebass.cx", "mouseears", "6665554455")

      val result = phantomUsersDao.updateSetting(user.id.get, SoundOnNewNotification, true)

      result must be equalTo (true)
    }

    "support updating an existing user's new picture notification settings" in withSetupTeardown {

      val user : PhantomUser = createVerifiedUser("deadmau5@wobblebass.cx", "mouseears", "6665554455")

      val result = phantomUsersDao.updateSetting(user.id.get, NotificationOnNewPicture, true)

      result must be equalTo (true)
    }

  }
}
