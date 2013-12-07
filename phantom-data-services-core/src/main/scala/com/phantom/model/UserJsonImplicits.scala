package com.phantom.model

import spray.json._

//order matters
object UserJsonImplicits extends DefaultJsonProtocol {

  implicit val userRegistrationFormat = jsonFormat3(UserRegistration)
  implicit val userLoginFormat = jsonFormat2(UserLogin)
  implicit val userInsertFormat = jsonFormat4(UserInsert)
  implicit val userFormat = jsonFormat5(User)

}
