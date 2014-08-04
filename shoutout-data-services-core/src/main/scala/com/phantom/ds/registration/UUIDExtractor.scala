package com.phantom.ds.registration

import java.util.UUID
import scala.util.Try
import com.phantom.model.RegistrationVerification
import com.phantom.ds.framework.Logging

object UUIDExtractor extends Logging {

  /**
   * This class will try to extractd a UUID from a text blob
   */

  private val ucharset = "[a-hA-H0-9]"
  private val regex = s"($ucharset{8}" + "\\-" + s"$ucharset{4}" + "\\-" + s"$ucharset{4}" + "\\-" + s"$ucharset{4}" + "\\-" + s"$ucharset{12})"
  private val UUIDM = regex.r

  def extractUUID(regVer : RegistrationVerification) : Option[UUID] = {
    UUIDM.findFirstIn(regVer.body).flatMap(validateUUID)
  }

  private def validateUUID(uuid : String) : Option[UUID] = {
    Try(Some(UUID.fromString(uuid))).getOrElse(None)
  }

}