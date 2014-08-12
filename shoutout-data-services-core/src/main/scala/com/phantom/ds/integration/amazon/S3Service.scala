package com.phantom.ds.integration.amazon

import com.phantom.ds.DSConfiguration
import org.jets3t.service.security.AWSCredentials
import org.jets3t.service.impl.rest.httpclient.RestS3Service
import org.jets3t.service.acl.{ Permission, GroupGrantee }
import org.jets3t.service.model.{ S3Bucket, S3Object }
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import org.joda.time.DateTime
import scala.concurrent.{ ExecutionContext, Future, future }

trait S3Service {

  def saveImage(image : Array[Byte]) : Future[String]
  def saveProfileImage(image : Array[Byte]) : String

}

object S3Service extends DSConfiguration {

  def apply()(implicit ex : ExecutionContext) = new S3Service {

    private lazy val s3 = {
      val awsAccessKey = AWS.accessKeyId
      val awsSecretKey = AWS.secretKey
      val awsCredentials = new AWSCredentials(awsAccessKey, awsSecretKey)
      new RestS3Service(awsCredentials)
    }

    private val profileBucketName = AWS.profileBucket
    private val shoutoutBucketName = AWS.shoutoutBucket

    private lazy val profileBucket = s3.getBucket(profileBucketName)
    private lazy val shoutBucket = s3.getBucket(shoutoutBucketName)

    private def saveFileToBucket(file : Array[Byte], bucket : S3Bucket, bucketName : String, contentType : String) : String = {

      val randomImageName : String = MessageDigest.getInstance("MD5").digest(DateTime.now().toString().getBytes).map("%02X".format(_)).mkString
      val url = randomImageName

      val fileObject = s3.putObject(bucket, {
        val acl = s3.getBucketAcl(bucket)
        acl.grantPermission(GroupGrantee.ALL_USERS, Permission.PERMISSION_READ)

        val tempObj = new S3Object(url)
        tempObj.setDataInputStream(new ByteArrayInputStream(file))
        tempObj.setAcl(acl)
        tempObj.setContentType(contentType)
        tempObj
      })

      s3.createUnsignedObjectUrl(bucketName,
        fileObject.getKey,
        false, false, false)

    }

    override def saveProfileImage(image : Array[Byte]) : String = {
      saveFileToBucket(image, profileBucket, profileBucketName, "image/jpg")
    }

    override def saveImage(image : Array[Byte]) : Future[String] = {
      future {
        saveFileToBucket(image, shoutBucket, shoutoutBucketName, "image/jpg")
      }
    }

  }
}