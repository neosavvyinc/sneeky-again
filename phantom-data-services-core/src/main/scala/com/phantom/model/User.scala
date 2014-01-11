package com.phantom.model

import com.phantom.dataAccess.Profile
import org.joda.time.LocalDate
import java.sql.Date
import scala.slick.lifted.ColumnOption.DBType

package object PhantomUserTypes {
  type PhoneNumber = String
}

case class UserRegistration(email : String,
                            birthday : LocalDate,
                            password : String)

case class UserLogin(email : String,
                     password : String)

case class ClientSafeUserResponse(email : String,
                                  phoneNumber : String,
                                  birthday : LocalDate,
                                  newPictureReceivedNotification : Boolean,
                                  soundsNotification : Boolean)

case class PhantomUserDeleteMe(id : String)

case class UserInsert(email : String,
                      birthday : LocalDate,
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
  import PhantomUserTypes._

  object UserTable extends Table[PhantomUser]("USERS") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def email = column[String]("EMAIL", DBType("VARCHAR(256)"))
    def birthday = column[LocalDate]("BIRTHDAY")
    def active = column[Boolean]("ACTIVE")
    def phoneNumber = column[PhoneNumber]("PHONE_NUMBER")

    def * = id.? ~ email ~ birthday ~ active ~ phoneNumber <> (PhantomUser, PhantomUser.unapply _)
    def forInsert = * returning id

  }
}
