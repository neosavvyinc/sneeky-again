package com.shoutout.ds.stats

import akka.actor.ActorRef
import com.shoutout.dataAccess.DatabaseSupport
import com.shoutout.ds.BasicCrypto
import com.shoutout.ds.framework.Logging
import com.shoutout.ds.integration.amazon.S3Service
import com.shoutout.model.{ StatsResponse, StatsRequest }
import scala.concurrent.{ Await, ExecutionContext, Future, future }
import scala.slick.session.Session

/**
 * Created by Dave on 8/25/14.
 */
trait StatsService {
  def getStats(request : StatsRequest) : Future[StatsResponse]
}

object StatsService extends BasicCrypto {

  def apply()(implicit ec : ExecutionContext) =
    new StatsService with DatabaseSupport with Logging {

      def getStats(request : StatsRequest) : Future[StatsResponse] = {
        future {
          if (request.password == "m02I7XkTFx") {
            db.withSession { implicit session : Session =>
              val unreadMessageCount = statsDao.sentAllCount
              val totalRegisterEmail = statsDao.registrationByEmailTotalCount
              val totalRegisterFacbeook = statsDao.registrationByFacebookTotalCount

              StatsResponse(
                sentAll = unreadMessageCount,
                regAllEmail = totalRegisterEmail,
                regAllFB = totalRegisterFacbeook
              )
            }
          } else {
            StatsResponse()
          }
        }
      }
    }
}
