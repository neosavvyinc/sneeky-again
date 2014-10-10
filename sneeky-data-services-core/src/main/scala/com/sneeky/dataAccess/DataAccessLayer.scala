package com.sneeky.dataAccess

import com.sneeky.model._

import scala.slick.driver.ExtendedProfile
import com.sneeky.ds.framework.Logging
import java.io.{ PrintWriter, File }

trait Profile {
  val profile : ExtendedProfile
}

class DataAccessLayer(override val profile : ExtendedProfile) extends Profile with Logging
    with UserComponent
    with SneekComponent
    with UserSessionComponent
    with LikeComponent
    with DislikeComponent {

}
