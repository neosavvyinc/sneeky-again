package com.phantom.model

import com.phantom.dataAccess.Profile
import org.joda.time.LocalDate
import java.sql.Date
import scala.slick.lifted.ColumnOption.DBType

case class UserRegistration(email : String,
                            birthday : String,
                            password : String)

case class UserLogin(email : String,
                     password : String)

case class ClientSafeUserResponse(email : String,
                                  phoneNumber : String,
                                  birthday : String,
                                  newPictureReceivedNotification : Boolean,
                                  soundsNotification : Boolean)

case class PhantomUserDeleteMe(id : String)

case class UserInsert(email : String,
                      birthday : String,
                      saltyHash : String,
                      active : Boolean)

// TO DO
// secret client-facing/obfuscated user id?
case class PhantomUser(id : Option[Long],
                       email : String,
                       birthday : LocalDate,
                       active : Boolean,
                       phoneNumber : String)

trait UserComponent { this : Profile =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  object UserTable extends Table[PhantomUser]("USERS") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def email = column[String]("EMAIL", DBType("VARCHAR(256)"))
    def birthday = column[LocalDate]("BIRTHDAY")
    def active = column[Boolean]("ACTIVE")
    def phoneNumber = column[String]("PHONE_NUMBER")

    def * = id.? ~ email ~ birthday ~ active ~ phoneNumber <> (PhantomUser, PhantomUser.unapply _)

  }
}

