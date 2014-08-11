package com.phantom.ds.integration.mock

import com.phantom.ds.integration.amazon.S3Service
import scala.concurrent.Future

class TestS3Service extends S3Service {
  override def saveProfileImage(image : Array[Byte]) : String = {
    "profileImageSaved"
  }

  override def saveImage(image : Array[Byte]) : Future[String] = {
    Future.successful(s"imageName")
  }
}