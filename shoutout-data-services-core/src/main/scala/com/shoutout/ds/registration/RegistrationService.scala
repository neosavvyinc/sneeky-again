package com.shoutout.ds.registration

import com.shoutout.model._
import scala.concurrent.{ ExecutionContext, Future, future }
import com.shoutout.dataAccess.DatabaseSupport
import com.shoutout.ds.framework.Logging
import com.shoutout.ds.user.Passwords
import java.util.UUID
import com.shoutout.model.RegistrationVerification
import com.shoutout.model.RegistrationResponse
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