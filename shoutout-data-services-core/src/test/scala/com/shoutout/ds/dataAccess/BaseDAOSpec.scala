package com.shoutout.ds.dataAccess

import org.specs2.mutable.Specification
import com.shoutout.dataAccess.DatabaseSupport
import org.specs2.specification.BeforeAfter
import com.shoutout.model._
import java.util.UUID
import com.shoutout.ds.user.Passwords
import com.shoutout.ds.TestUtils
import scala.concurrent.ExecutionContext.Implicits.global
import scala.slick.session.Session
import com.shoutout.ds.framework.Dates

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
