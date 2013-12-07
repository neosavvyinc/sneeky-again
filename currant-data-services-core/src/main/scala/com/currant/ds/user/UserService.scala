package com.currant.ds.user

import com.currant.model._
import scala.concurrent.{ ExecutionContext, Future }
import com.currant.ds.db.DB
import scala.concurrent.future
import com.currant.model.CurrantUserInsert
import com.currant.model.ProfileInsert
import com.currant.model.CurrantUserRegistration

trait UserService {

  def registerUser(registrationRequest : CurrantUserRegistration) : Future[InsertResponse]

}

class DuplicateUserException(message : String = "Email already in use") extends Exception(message) {
  val code = 101
}

object UserService {

  def apply(db : DB)(implicit ec : ExecutionContext) = new UserService {

    def registerUser(req : CurrantUserRegistration) : Future[InsertResponse] = {
      future {
        db.withTransactionContext { implicit ctx =>

          val emailExists = UserCRUD.emailCheck(req.email).execute() > 0
          if (emailExists) {
            throw new DuplicateUserException()
          }
          val saltyHash = Passwords.getSaltedHash(req.password)
          val userInsert = CurrantUserInsert(req.email, saltyHash, Active, "", true)
          val id = UserCRUD.insertUser(userInsert)
          val source = req.facebookId.map(x => Facebook).getOrElse(Currant)
          val profileInsert = ProfileInsert(id, source, req.facebookId.getOrElse(""), req.firstName, req.lastName, Standard, req.favoriteTimeToPlay)
          val profileId = UserCRUD.insertProfile(profileInsert)
          UserCRUD.insertFavoriteSports(profileId, req.favoriteSports)
          InsertResponse(id, profileId)
        }
      }
    }

  }

}