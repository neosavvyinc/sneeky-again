
package com.phantom.ds.dataAccess

class PhantomUsersDAOSpec extends BaseDAOSpec {

  sequential

  "PhantomUserDAO" should {

    "support finding a users contacts by phone number" in withSetupTeardown {

      insertUsersWithPhoneNumbersAndContacts

      val res = phantomUsersDao.findPhantomUserIdsByPhone(List("5192050", "2061266"))

      res must be_==((List[Long](2, 3), List[String]())).await
    }

    "return a tuple of ids and non-found phone numbers" in withSetupTeardown {

      insertUsersWithPhoneNumbersAndContacts

      val res = phantomUsersDao.findPhantomUserIdsByPhone(List("5192050", "2061266", "7777777"))

      res must be_==((List[Long](2, 3), List[String]("7777777"))).await
    }
  }
}
