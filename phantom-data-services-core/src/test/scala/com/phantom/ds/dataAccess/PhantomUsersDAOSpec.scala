
package com.phantom.ds.dataAccess

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
  }
}
