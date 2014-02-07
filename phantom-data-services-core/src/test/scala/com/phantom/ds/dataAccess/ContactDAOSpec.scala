package com.phantom.ds.dataAccess

import com.phantom.model.Contact

class ContactDAOSpec extends BaseDAOSpec {

  sequential

  "ContactDAO" should {
    "support inserting a single contact" in withSetupTeardown {
      insertTestUsers
      contacts.insert(Contact(None, 1, 2, "friend")) must be_==(Contact(Some(1), 1, 2, "friend")).await
      contacts.insert(Contact(None, 2, 1, "friend")) must be_==(Contact(Some(2), 2, 1, "friend")).await
    }

    "support inserting a list of contacts" in withSetupTeardown {

      insertTestUsers

      val cs : List[Contact] = List(
        Contact(None, 1, 2, "friend"),
        Contact(None, 1, 3, "friend")
      )

      val res = contacts.insertAll(cs)

      res must be_==(
        List(
          Contact(Some(1), 1, 2, "friend"),
          Contact(Some(2), 1, 3, "friend")
        )
      )

      contacts.findAll must be_==(
        List(
          Contact(Some(1), 1, 2, "friend"),
          Contact(Some(2), 1, 3, "friend")
        )
      )
    }

    "should support deleting a user's contacts" in withSetupTeardown {

      val session = db.createSession

      val cs : List[Contact] = List(
        Contact(None, 1, 2, "friend"),
        Contact(None, 1, 3, "friend")
      )

      insertTestUsers
      contacts.insertAll(cs)

      contacts.deleteAll(1)(session) must be_==(2)
    }

    "should support finding a contact by contactId" in withSetupTeardown {
      insertTestUsers
      contacts.insert(Contact(None, 2, 3, "friend"))

      contacts.findByContactId(2, 3) must be_==(Contact(Some(1), 2, 3, "friend")).await

    }

    "should support updating a contact" in withSetupTeardown {
      insertTestUsers
      contacts.insert(Contact(None, 2, 3, "friend"))

      contacts.update(Contact(Some(1), 2, 3, "block")) must be_==(1).await
      contacts.findAll must be_==(List(Contact(Some(1), 2, 3, "block"))).await
    }
  }
}
