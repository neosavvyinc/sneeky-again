package com.phantom.ds.registration

import com.phantom.model._
import scala.concurrent.{ ExecutionContext, Future, future }
import com.phantom.dataAccess.DatabaseSupport
import com.phantom.ds.framework.Logging
import com.phantom.ds.user.Passwords
import java.util.UUID
import com.phantom.model.RegistrationVerification
import com.phantom.model.RegistrationResponse
import com.phantom.model.UserRegistration
import scala.slick.session.Session

trait RegistrationService {

  def hello() : String
}

object RegistrationService {

  def apply()(implicit ec : ExecutionContext) =
    new RegistrationService with DatabaseSupport with Logging {

      def hello() : String = {
        "Hello"
      }

    }
}