package com.phantom.ds.dataAccess

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

    "Dumbest assertion possible" in withSetupTeardown {

      true should be equalTo true

    }

  }

}
