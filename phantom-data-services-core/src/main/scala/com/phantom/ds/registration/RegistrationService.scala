package com.phantom.ds.registration

import com.phantom.model.{ RegistrationVerification, PhantomSession, RegistrationResponse, UserRegistration }
import scala.concurrent.{ ExecutionContext, Future }
import com.phantom.dataAccess.DatabaseSupport
import com.phantom.ds.framework.Logging
import com.phantom.ds.user.Passwords
import java.util.UUID

trait RegistrationService {

  def register(registrationRequest : UserRegistration) : Future[RegistrationResponse]
  def verifyRegistration(response : RegistrationVerification) : Future[Unit]
}

object RegistrationService {

  def apply()(implicit ec : ExecutionContext) =
    new RegistrationService with DatabaseSupport with Logging {

      def register(registrationRequest : UserRegistration) : Future[RegistrationResponse] = {
        for {
          _ <- Passwords.validate(registrationRequest.password)
          user <- phantomUsersDao.register(registrationRequest)
          session <- sessions.createSession(PhantomSession.newSession(user))
        } yield RegistrationResponse(user.uuid, session.sessionId)
      }

      def verifyRegistration(response : RegistrationVerification) : Future[Unit] = {
        log.error(s"received $response")
        val uuidOpt = UUIDExtractor.extractUUID(response)

        log.debug(s"receied the uuid: " + uuidOpt)

        uuidOpt.map(updateUserStatus(_, response)).getOrElse(logBadVerification(response))
      }

      private def updateUserStatus(uuid : UUID, message : RegistrationVerification) : Future[Unit] = {
        val updated = phantomUsersDao.verifyUser(uuid, message.from)
        updated.map { x =>
          if (x != 1) {
            log.error(s"uuid : $uuid extracted from $message is either not valid, or the user is already verified.")
          }
        }
      }

      private def logBadVerification(response : RegistrationVerification) : Future[Unit] = {
        Future.successful(log.error(s"invalid UUID detected for $response"))
      }

    }
}