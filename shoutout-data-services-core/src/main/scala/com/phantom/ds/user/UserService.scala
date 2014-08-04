package com.phantom.ds.user

import scala.concurrent.{ ExecutionContext, Future, future }
import com.phantom.model._
import com.phantom.ds.framework.Logging
import com.phantom.model.UserLogin
import com.phantom.model.PhantomUser
import com.phantom.dataAccess.DatabaseSupport
import java.util.UUID
import com.phantom.ds.framework.exception.PhantomException
import com.phantom.ds.framework.email.{ MandrillConfiguration, MandrillUtil }
import com.phantom.ds.BasicCrypto
import scala.slick.session.Session

trait UserService {

  def hello() : String

}

object UserService extends BasicCrypto {

  def apply()(implicit ec : ExecutionContext) = new UserService with DatabaseSupport with Logging {

    def hello() : String = {
      "hello"
    }

  }

}

