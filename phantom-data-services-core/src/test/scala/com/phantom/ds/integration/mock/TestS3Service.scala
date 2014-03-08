package com.phantom.ds.integration.mock

import com.phantom.ds.integration.amazon.S3Service

class TestS3Service extends S3Service {
  override def saveImage(image : Array[Byte], conversationId : Long) : String = {
    s"imageName$conversationId"
  }
}