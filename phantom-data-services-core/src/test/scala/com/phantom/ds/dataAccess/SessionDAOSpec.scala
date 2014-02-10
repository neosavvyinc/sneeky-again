package com.phantom.ds.dataAccess

import java.util.UUID
import com.phantom.model._
import scala.concurrent.ExecutionContext.Implicits.global
import com.phantom.model.PhantomUser

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

      val session = await {
        sessions.createSession(PhantomSession.newSession(user))
      }

      val result = sessions.updatePushNotifier(session.sessionId, "motorheadrocks", Apple)

      result should be equalTo true
    }

  }

}
