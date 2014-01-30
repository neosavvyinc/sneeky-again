package com.phantom.ds.registration

import java.util.UUID
import scala.util.Try
import com.phantom.model.RegistrationVerification

object UUIDExtractor {

  private val regex = "##"

  def extractUUID(regVer : RegistrationVerification) : Option[UUID] = {

    val split : List[String] = regVer.body.split(regex).toList
    split match {
      case x :: y :: z :: Nil => validateUUID(y)
      case _                  => None
    }
  }

  private def validateUUID(uuid : String) : Option[UUID] = {
    Try(Some(UUID.fromString(uuid))).getOrElse(None)
  }

}