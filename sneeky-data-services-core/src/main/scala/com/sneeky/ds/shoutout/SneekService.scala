package com.sneeky.ds.shoutout

import com.netaporter.i18n.ResourceBundle

import scala.concurrent.{ Future, ExecutionContext, future }
import com.sneeky.model._
import com.sneeky.dataAccess.DatabaseSupport
import com.sneeky.ds.{ BasicCrypto, DSConfiguration }
import com.sneeky.ds.framework.{ Dates, Logging }
import akka.actor.ActorRef
import com.sneeky.ds.integration.apple.AppleNotification
import scala.slick.session.Session
import java.util.{ Locale, UUID }

import com.sneeky.ds.integration.amazon.S3Service

import scala.util.Try

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 12/7/13
 * Time: 2:01 PM
 */
trait SneekService {

  def saveData(data : Option[Array[Byte]], contentType : String) : Future[String]
  def sendToRecipients(sender : SneekyV2User, url : String, imageText : Option[String], contentType : String) : Unit

}

object SneekService extends DSConfiguration with BasicCrypto {

  def apply(appleActor : ActorRef, s3Service : S3Service)(implicit ec : ExecutionContext) =
    new SneekService with DatabaseSupport with Logging {

      val resourceBundle = ResourceBundle("messages/messages")

      def saveData(data : Option[Array[Byte]], contentType : String) : Future[String] = {
        data match {
          case None => future {
            ""
          }
          case Some(x) => s3Service.saveData(x, contentType)
        }

      }

      def sendToRecipients(sender : SneekyV2User, url : String, text : Option[String], contentType : String) : Unit = {

        var returnVal = 0

        db.withTransaction { implicit s : Session =>

          // insert a record for each user into the Shoutout table that isn't blocked
          sneekyDao.insertSneek(Sneek(
            None,
            sender.id.get,
            text.getOrElse(""),
            url,
            Dates.nowDT
          ))

        }
      }
    }
}
