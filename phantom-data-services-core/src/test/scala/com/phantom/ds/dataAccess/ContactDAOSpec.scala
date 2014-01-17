package com.phantom.ds.dataAccess

import org.specs2.mutable._
import com.phantom.dataAccess.DatabaseSupport
import com.phantom.model.{ PhantomUser, Contact, ContactComponent }
import org.specs2.specification.BeforeAfter

class ContactDAOSpec extends BaseDAOSpec {

  sequential

  "ContactDAO" should {
    "support inserting a list of contacts" in withSetupTeardown {

      insertTestUsers

      val res = contacts.insertList(1, List(2, 3))

      val cs : List[Contact] = List(
        Contact(Some(1), 1, 2, "friend"),
        Contact(Some(2), 1, 3, "friend")
      )
      res must be_==(cs).await
    }
  }
}
