package com.phantom.ds.dataAccess

import org.specs2.mutable.Specification
import com.phantom.dataAccess.DatabaseSupport
import org.specs2.specification.BeforeAfter

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 1/9/14
 * Time: 9:34 PM
 * To change this template use File | Settings | File Templates.
 */
class BaseDAOSpec extends Specification with DatabaseSupport {

  object withSetupTeardown extends BeforeAfter {
    def before {
      println("Executing before stuff...")
      conversations.dropDB
      conversations.createDB

    }

    def after {
      println("Executing after astuff...")
      conversations.dropDB
    }
  }

}
