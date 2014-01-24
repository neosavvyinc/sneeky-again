package com.phantom.ds.dataAccess

import org.specs2.mutable._
import com.phantom.dataAccess.DatabaseSupport
import com.phantom.model.{ PhantomUser, Contact, ContactComponent }
import org.specs2.specification.BeforeAfter

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

      val res = contacts.insertList(1, List(2, 3))

      val cs : List[Contact] = List(
        Contact(Some(1), 1, 2, "friend"),
        Contact(Some(2), 1, 3, "friend")
      )
      res must be_==(cs).await

      contacts.findAll must be_==(cs).await
    }

    "should support deleting a user's contacts" in withSetupTeardown {

      val session = db.createSession

      insertTestUsers
      contacts.insertList(1, List(2, 3))

      contacts.deleteAll(1)(session) must be_==(2).await
    }
  }
}
