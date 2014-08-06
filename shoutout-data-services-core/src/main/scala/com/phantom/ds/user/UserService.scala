package com.phantom.ds.user

import scala.concurrent.{ ExecutionContext, Future, future }
import com.phantom.model._
import com.phantom.ds.framework.Logging
import com.phantom.model.UserLogin
import com.phantom.model.ShoutoutUser
import com.phantom.dataAccess.DatabaseSupport
import java.util.UUID
import com.phantom.ds.framework.exception.ShoutoutException
import com.phantom.ds.framework.email.{ MandrillConfiguration, MandrillUtil }
import com.phantom.ds.BasicCrypto
import scala.slick.session.Session

trait UserService {

  def login(loginRequest : UserLogin) : Future[LoginSuccess]
  def facebookLogin(loginRequest : FacebookUserLogin) : Future[LoginSuccess]
  def register(registrationRequest : UserRegistrationRequest) : Future[RegistrationResponse]
  def updateUser(userId : Int, updateRequest : ShoutoutUserUpdateRequest) : Future[ShoutoutUser]

}

object UserService extends BasicCrypto {

  def apply()(implicit ec : ExecutionContext) = new UserService with DatabaseSupport with Logging {

    def login(loginRequest : UserLogin) : Future[LoginSuccess] = {
      for {
        user <- shoutoutUsersDao.login(loginRequest)
        session <- sessions.createSession(ShoutoutSession.newSession(user))
      } yield LoginSuccess(session.sessionId)
    }

    def facebookLogin(loginRequest : FacebookUserLogin) : Future[LoginSuccess] = {
      for {
        user <- shoutoutUsersDao.loginByFacebook(loginRequest)
        session <- sessions.createSession(ShoutoutSession.newSession(user))
      } yield LoginSuccess(session.sessionId)
    }

    def register(registrationRequest : UserRegistrationRequest) : Future[RegistrationResponse] = {
      for {
        _ <- Passwords.validate(registrationRequest.password)
        registrationResponse <- doRegistration(registrationRequest)
      } yield registrationResponse
    }

    def logout(sessionId : String) : Future[Int] = {
      sessions.removeSession(UUID.fromString(sessionId))
    }

    def updateUser(userId : Int, updateRequest : ShoutoutUserUpdateRequest) : Future[ShoutoutUser] = {

      for {
        persistentUser <- shoutoutUsersDao.findById(userId)
        updatedUser <- shoutoutUsersDao.update(persistentUser, updateRequest)
      } yield updatedUser
    }

    private def doRegistration(registrationRequest : UserRegistrationRequest) : Future[RegistrationResponse] = {
      future {
        db.withTransaction { implicit s =>
          val user = shoutoutUsersDao.registerOperation(registrationRequest)
          val session = sessions.createSessionOperation(ShoutoutSession.newSession(user))
          RegistrationResponse(session.sessionId)
        }
      }
    }

  }

}

