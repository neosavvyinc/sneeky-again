package com.shoutout.ds.user

import com.shoutout.ds.integration.amazon.S3Service

import scala.concurrent.{ Await, ExecutionContext, Future, future }
import com.shoutout.model._
import com.shoutout.ds.framework.Logging
import com.shoutout.model.UserLogin
import com.shoutout.model.ShoutoutUser
import com.shoutout.dataAccess.DatabaseSupport
import java.util.UUID
import com.shoutout.ds.framework.exception.ShoutoutException
import com.shoutout.ds.framework.email.{ MandrillConfiguration, MandrillUtil }
import com.shoutout.ds.BasicCrypto
import scala.slick.session.Session

trait UserService {

  def login(loginRequest : UserLogin) : Future[LoginSuccess]
  def facebookLogin(loginRequest : FacebookUserLogin) : Future[LoginSuccess]
  def register(registrationRequest : UserRegistrationRequest) : Future[RegistrationResponse]
  def logout(sessionId : String) : Future[Int]
  def updateUser(userId : Long, updateRequest : ShoutoutUserUpdateRequest) : Future[Int]
  def findFromSessionId(sessionId : String) : Future[ShoutoutSession]
  def updatePushNotifier(sessionUUID : UUID, applePushToken : String, mobilePushType : MobilePushType) : Future[Boolean]

}

object UserService extends BasicCrypto {

  def apply(s3Service : S3Service)(implicit ec : ExecutionContext) = new UserService with DatabaseSupport with Logging {

    def login(loginRequest : UserLogin) : Future[LoginSuccess] = {
      for {
        user <- shoutoutUsersDao.login(UserLogin(
          decryptField(loginRequest.email),
          decryptField(loginRequest.password)
        ))
        session <- sessionsDao.createSession(ShoutoutSession.newSession(user))
      } yield LoginSuccess(session.sessionId)
    }

    def facebookLogin(loginRequest : FacebookUserLogin) : Future[LoginSuccess] = {

      def insertFriendIfNotExists(user : ShoutoutUser, targetFriendId : Long) = {
        db.withSession { implicit session : Session =>
          val contactOption = contactsDao.findContactByIdForOwner(user, 1)
          contactOption match {
            case None    => contactsDao.insertFriendAssociation(user, ContactOrdering(None, Some(1), FriendType), 0)
            case Some(c) => log.debug(s"User $user is already friends with the shoutout team")
          }

        }
      }

      var decryptedLoginRequest = FacebookUserLogin(
        decryptField(loginRequest.facebookId),
        loginRequest.firstName,
        loginRequest.lastName,
        loginRequest.birthdate
      )

      for {
        user <- shoutoutUsersDao.loginByFacebook(decryptedLoginRequest)
        session <- {
          insertFriendIfNotExists(user, 1)
          sessionsDao.createSession(ShoutoutSession.newSession(user))
        }
      } yield LoginSuccess(session.sessionId)
    }

    def register(registrationRequest : UserRegistrationRequest) : Future[RegistrationResponse] = {
      val decryptedRequest = UserRegistrationRequest(
        decryptField(registrationRequest.email),
        decryptField(registrationRequest.password)
      )

      for {
        _ <- Passwords.validate(decryptedRequest.password)
        registrationResponse <- doRegistration(decryptedRequest)
      } yield registrationResponse
    }

    def logout(sessionId : String) : Future[Int] = {
      sessionsDao.removeSession(UUID.fromString(sessionId))
    }

    def findFromSessionId(sessionId : String) : Future[ShoutoutSession] = {
      sessionsDao.sessionByUUID(UUID.fromString(sessionId))
    }

    def updateUser(userId : Long, updateRequest : ShoutoutUserUpdateRequest) : Future[Int] = {

      def validate() : Future[Boolean] = {
        future {

          updateRequest.username match {
            case Some(x) => {
              val isRestricted = shoutoutUsersDao.isUserNameRestricted(x)
              if (isRestricted)
                throw ShoutoutException.restrictedUsernameException
              else
                isRestricted
            }
            case _ => true
          }

        }
      }

      for {
        _ <- validate()
        persistentUser <- shoutoutUsersDao.findById(userId)
        rowsUpdated <- shoutoutUsersDao.update(persistentUser, updateRequest)
      } yield rowsUpdated

    }

    def updateUserPhoto(image : Array[Byte], user : ShoutoutUser) : Future[String] = {

      def saveUserPhotoForProfile(image : Array[Byte]) : String = {
        s3Service.saveProfileImage(image)
      }

      future {
        val url = saveUserPhotoForProfile(image)
        shoutoutUsersDao.updateProfilePicUrl(user.copy(profilePictureUrl = Some(url)))
        url
      }

    }

    def updateSetting(userId : Long, pushSettingType : SettingType, value : Boolean) : Future[Boolean] = {
      future {
        shoutoutUsersDao.updateSetting(userId, pushSettingType, value)
      }
    }

    def deriveExtraProperties(user : ShoutoutUser, activeUser : ActiveShoutoutUser, shoutoutSession : ShoutoutSession) : ActiveShoutoutUser = {

      db.withSession { implicit session : Session =>
        activeUser.copy(
          profilePictureUrl = encryptField(activeUser.profilePictureUrl),
          receivedCount = shoutoutDao.countReceived(user),
          sentCount = shoutoutDao.countSent(user),
          sessionInvalid = shoutoutSession.sessionInvalid)
      }

    }

    def updatePushNotifier(sessionUUID : UUID, applePushToken : String, mobilePushType : MobilePushType) : Future[Boolean] = {
      future {

        val sessions = sessionsDao.findFromPushNotifierAndType(applePushToken, mobilePushType)
        sessions foreach (s => {
          sessionsDao.updatePushNotifier(s.sessionId, null, s.pushNotifierType.getOrElse(NullType))
        })

        sessionsDao.updatePushNotifier(sessionUUID, applePushToken, mobilePushType)
      }
    }

    def forgotPassword(email : String) : Future[Boolean] = {
      for {
        status <- resetPassword(decryptField(email))
        results <- sendResetPasswordEmail(status)
      } yield results

    }

    def changePassword(user : ShoutoutUser, request : ChangePasswordRequest) : Future[Boolean] = {

      future {
        db.withTransaction {
          implicit s =>

            val userFromDB = shoutoutUsersDao.findByIdOperation(user.id.get)
            val isValid : Boolean = userFromDB match {
              case Some(u) => Passwords.check(decryptField(request.oldPassword), decryptField(u.password.getOrElse(throw ShoutoutException.genericPasswordException)))
              case None    => throw ShoutoutException.genericPasswordException
            }

            if (isValid) {
              shoutoutUsersDao.updatePasswordForUserOperation(user.id.get, Passwords.getSaltedHash(decryptField(request.newPassword)))
            } else {
              throw ShoutoutException.genericPasswordException
            }
        }
      }

    }

    private def doRegistration(registrationRequest : UserRegistrationRequest) : Future[RegistrationResponse] = {
      future {
        db.withTransaction { implicit s =>
          val user = shoutoutUsersDao.registerOperation(registrationRequest)
          val session = sessionsDao.createSessionOperation(ShoutoutSession.newSession(user))

          //This one liner adds an association to the team shoutout account
          contactsDao.insertFriendAssociation(user, ContactOrdering(None, Some(1), FriendType), 0)
          RegistrationResponse(session.sessionId)
        }
      }
    }

    private def resetPassword(email : String) : Future[Option[ResetPasswordResults]] = {
      future {
        db.withTransaction { implicit session =>
          val userOpt = shoutoutUsersDao.findByEmailOperation(email)
          userOpt.map { user =>
            val newPassword = Passwords.generateNewPassword().substring(0, 8)
            val encrypted = Passwords.getSaltedHash(newPassword)
            shoutoutUsersDao.updatePasswordForUserOperation(email, encrypted)
            val invalidatedSessions = sessionsDao.invalidateAllForUser(user.id.get)
            log.trace(s"There were $invalidatedSessions invalidated for user by email: $email ")
            ResetPasswordResults(email, newPassword)
          }
        }
      }
    }

    private def sendResetPasswordEmail(status : Option[ResetPasswordResults]) : Future[Boolean] = {
      status match {
        case None => Future.successful(false)
        case Some(x) => {
          future {
            MandrillUtil.sendMailViaMandrill(
              new MandrillConfiguration(
                MandrillConfiguration.apiKey,
                MandrillConfiguration.smtpPort,
                MandrillConfiguration.smtpHost,
                MandrillConfiguration.username
              ), x.email, x.password)
            true
          }
        }
      }
    }

    private[this] case class ResetPasswordResults(email : String, password : String)
  }

}

