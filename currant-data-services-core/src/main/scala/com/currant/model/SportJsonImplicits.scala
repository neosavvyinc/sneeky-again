package com.currant.model

import spray.json._

object SportJsonImplicits extends DefaultJsonProtocol {

  implicit val sportFormat = jsonFormat8(Sport)

  implicit val sportCreateRequestFormat = jsonFormat7(SportCreateRequest)

}