package com.phantom.ds.dataAccess

import java.util.UUID
import com.phantom.model.{ PhantomUser, PhantomSession }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 2/6/14
 * Time: 7:56 PM
 * To change this template use File | Settings | File Templates.
 */
class SessionDAOSpec extends BaseDAOSpec {

  sequential

  "PhantomUserDAO" should {

    "support updating an existing session with a push notifier" in withSetupTeardown {

      val user : PhantomUser = createVerifiedUser("lemmy@kilmister.com", "motorhead", "9998887766")

      System.out.println("am i here")

      val session = await {
        sessions.createSession(PhantomSession.newSession(user))
      }

      System.out.println("am i here again")

      val result = sessions.updatePushNotifier(session.sessionId, "motorheadrocks")

      System.out.println("am i here three times")

      result should be equalTo true
    }

  }

}
