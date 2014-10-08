package com.sneeky.ds.framework

import java.util.UUID

import scala.util.Try

object UUIDExtractor extends Logging {

  /**
   * This class will try to extractd a UUID from a text blob
   */

  private val ucharset = "[a-hA-H0-9]"
  private val regex = s"($ucharset{8}" + "\\-" + s"$ucharset{4}" + "\\-" + s"$ucharset{4}" + "\\-" + s"$ucharset{4}" + "\\-" + s"$ucharset{12})"
  private val UUIDM = regex.r

  private def validateUUID(uuid : String) : Option[UUID] = {
    Try(Some(UUID.fromString(uuid))).getOrElse(None)
  }

}