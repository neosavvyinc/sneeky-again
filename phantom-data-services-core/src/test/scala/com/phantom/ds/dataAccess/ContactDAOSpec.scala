package com.phantom.ds.dataAccess

import com.phantom.model.{ Blocked, Contact }

class ContactDAOSpec extends BaseDAOSpec {

  sequential

  "ContactDAO" should {
    "support inserting a single contact" in withSetupTeardown {
      insertTestUsers()
      contacts.insert(Contact(None, 1, 2)) must be_==(Contact(Some(1), 1, 2)).await
      contacts.insert(Contact(None, 2, 1)) must be_==(Contact(Some(2), 2, 1)).await
    }

    "support inserting a list of contacts" in withSetupTeardown {

      insertTestUsers()

      val cs : List[Contact] = List(
        Contact(None, 1, 2),
        Contact(None, 1, 3)
      )

      val res = contacts.insertAll(cs)

      res must be_==(
        List(
          Contact(Some(1), 1, 2),
          Contact(Some(2), 1, 3)
        )
      )

      contacts.findAll must be_==(
        List(
          Contact(Some(1), 1, 2),
          Contact(Some(2), 1, 3)
        )
      )
    }

    "should support deleting a user's contacts" in withSetupTeardown {

      val session = db.createSession()

      val cs : List[Contact] = List(
        Contact(None, 1, 2),
        Contact(None, 1, 3)
      )

      insertTestUsers()
      contacts.insertAll(cs)

      contacts.deleteAll(1)(session) must be_==(2)
    }

    "should support finding a contact by contactId" in withSetupTeardown {
      insertTestUsers()
      contacts.insert(Contact(None, 2, 3))

      contacts.findByContactId(2, 3) must be_==(Contact(Some(1), 2, 3)).await

    }

    "should support updating a contact" in withSetupTeardown {
      insertTestUsers()
      contacts.insert(Contact(None, 2, 3))

      contacts.update(Contact(Some(1), 2, 3, Blocked)) must be_==(1).await
      contacts.findAll must be_==(List(Contact(Some(1), 2, 3, Blocked)))
    }
  }
}
