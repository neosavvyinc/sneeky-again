package com.phantom.ds.registration

import org.specs2.mutable.Specification
import java.util.UUID
import com.phantom.ds.TestUtils

class UUIDExtractorSpec extends Specification with TestUtils {

  "The UUIDExtractor" should {

    "fail if the message is not in the proper form of 'text[UUID]text" in {
      failAllCases(bad)
      failAllCases(valid.toString)
      1 must beEqualTo(1)
    }

    "pass if the UUID contained is valid" in {
      UUIDExtractor.extractUUID(reg("pre", valid.toString, "post")) must beSome(valid)
    }
  }

  val valid = UUID.randomUUID()
  val bad = "baduuid"

  private def failAllCases(uuid : String) {
    assertFailure("", bad, "")
    assertFailure("pre", bad, "")
    assertFailure("", bad, "post")
    assertFailure("pre", bad, "post")
  }

  private def assertFailure(pre : String, uuid : String, post : String) {
    UUIDExtractor.extractUUID(reg(pre, uuid, post)) must beNone
  }

}