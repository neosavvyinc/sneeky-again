package com.phantom.ds.registration

import java.util.UUID
import scala.util.Try
import com.phantom.model.RegistrationVerification
import com.phantom.ds.framework.Logging

object UUIDExtractor extends Logging {

  /**
   * This regex will need to change. Currently the following message:
   *  "This is a message ##UUID## end of message" generates a positive result
   * However, this one:
   *  "This is a message ##UUID##" generates a negative result.
   *
   * Aside from that, the message format will be like this in Sneaky:
   *
   * "This is a message to the user [UUID]"
   */
  private val regex = """[\[\]]"""

  def extractUUID(regVer : RegistrationVerification) : Option[UUID] = {

    val split : List[String] = regVer.body.split(regex).toList

    log.debug(split.toString())

    split match {
      case x :: y :: z :: Nil => validateUUID(y)
      case _                  => None
    }
  }

  private def validateUUID(uuid : String) : Option[UUID] = {
    Try(Some(UUID.fromString(uuid))).getOrElse(None)
  }

}