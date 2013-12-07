package com.phantom.model

import spray.json._

//order matters
object UserJsonImplicits extends DefaultJsonProtocol {

  implicit val userRegistrationFormat = jsonFormat3(UserRegistration)

}