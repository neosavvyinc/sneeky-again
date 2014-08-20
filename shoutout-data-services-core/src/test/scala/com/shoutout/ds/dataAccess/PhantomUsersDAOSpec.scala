
package com.shoutout.ds.dataAccess

import com.shoutout.model.{ NewMessagePushNotifications, ShoutoutUser }

class PhantomUsersDAOSpec extends BaseDAOSpec {

  sequential

  "PhantomUserDAO" should {

    "Dumbest assertion possible" in withSetupTeardown {

      true should be equalTo true

    }

  }
}
