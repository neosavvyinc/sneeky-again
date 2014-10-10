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
trait ShoutoutService {

  def saveData(data : Array[Byte], contentType : String) : Future[String]
  def sendToRecipients(sender : SneekyV2User, url : String, imageText : Option[String], contentType : String) : Unit
  def findAllForUser(user : SneekyV2User, sessionId : UUID) : Future[List[ShoutoutResponse]]
  def updateShoutoutAsViewedForUser(user : SneekyV2User, id : Long) : Future[Int]

}

object ShoutoutService extends DSConfiguration with BasicCrypto {

  def apply(appleActor : ActorRef, s3Service : S3Service)(implicit ec : ExecutionContext) =
    new ShoutoutService with DatabaseSupport with Logging {

      val resourceBundle = ResourceBundle("messages/messages")

      def saveData(data : Array[Byte], contentType : String) : Future[String] = {
        s3Service.saveData(data, contentType)
      }

      val codes = List(
        "\ue415",
        "\ue057",
        "\ue404",
        "\ue011",
        "\ue052",
        "\ue048",
        "\ue531",
        "\ue050",
        "\ue051",
        "\ue528",
        "\ue109",
        "\ue11b",
        "\ue312",
        "\ue112",
        "\ue310",
        "\ue12b",
        "\ue120",
        "\ue33b",
        "\ue33a",
        "\ue34b",
        "\ue10d"
      )

      val randomNumber = scala.util.Random

      def sendToRecipients(sender : SneekyV2User, url : String, text : Option[String], contentType : String) : Unit = {

        var returnVal = 0

        db.withTransaction { implicit s : Session =>

          // insert a record for each user into the Shoutout table that isn't blocked
          sneekyDao.insertSneek(Shoutout(
            None,
            sender.id.get,
            0,
            text.getOrElse(""),
            url,
            false,
            None,
            Dates.nowDT,
            false,
            contentType
          ))

        }
      }

      def findAllForUser(user : SneekyV2User, sessionId : UUID) : Future[List[ShoutoutResponse]] = {
        future {
          db.withSession { implicit s : Session =>
            sessionsDao.updateLastAccessed(sessionId)
          }
        }

        future {
          db.withSession { implicit s : Session =>
            sneekyUserDao.updateLastAccessedOperation(user)
          }
        }

        future {
          db.withSession { implicit s : Session =>
            sneekyDao.findAllForUser(user) map { resp =>
              resp.copy(
                imageUrl = encryptField(resp.imageUrl)
              )
            }
          }
        }
      }

      def updateShoutoutAsViewedForUser(user : SneekyV2User, id : Long) : Future[Int] = {

        future {
          db.withTransaction { implicit s : Session =>
            sneekyDao.setViewed(user, id)
          }
        }

      }
    }
}
