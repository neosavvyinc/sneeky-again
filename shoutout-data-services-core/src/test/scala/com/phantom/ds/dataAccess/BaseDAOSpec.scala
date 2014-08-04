package com.phantom.ds.dataAccess

import org.specs2.mutable.Specification
import com.phantom.dataAccess.DatabaseSupport
import org.specs2.specification.BeforeAfter
import com.phantom.model._
import java.util.UUID
import com.phantom.ds.user.Passwords
import com.phantom.ds.TestUtils
import scala.concurrent.ExecutionContext.Implicits.global
import scala.slick.session.Session
import com.phantom.ds.framework.Dates

/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 1/9/14
 * Time: 9:34 PM
 * To change this template use File | Settings | File Templates.
 */
trait BaseDAOSpec extends Specification with DatabaseSupport with TestUtils {

  object withSetupTeardown extends BeforeAfter {
    def before {
      dataAccessLayer.drop(db)
      dataAccessLayer.create(db)
    }

    def after {
      dataAccessLayer.drop(db)
    }
  }

}
