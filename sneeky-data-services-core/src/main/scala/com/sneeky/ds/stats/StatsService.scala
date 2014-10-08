package com.sneeky.ds.stats

import akka.actor.ActorRef
import com.sneeky.dataAccess.DatabaseSupport
import com.sneeky.ds.BasicCrypto
import com.sneeky.ds.framework.Logging
import com.sneeky.ds.integration.amazon.S3Service
import com.sneeky.model.{ StatsResponse, StatsRequest }
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
          if (request.password == "k4WxG9ySYz3nhZjuQrwFm2sn") {
            db.withSession { implicit session : Session =>
              val sentTodayCount = statsDao.sentCountForDate(request.date, request.timezone)
              val sentAllCount = statsDao.sentAllCount
              val todayRegisterEmailCount = statsDao.registrationByEmailTotalCountForDate(request.date, request.timezone)
              val totalRegisterEmailCount = statsDao.registrationByEmailTotalCount
              val todayRegisterFacebookCount = statsDao.registrationByFacebookTotalCountForDate(request.date, request.timezone)
              val totalRegisterFacebookCount = statsDao.registrationByFacebookTotalCount

              StatsResponse(
                sentToday = sentTodayCount,
                sentAll = sentAllCount,
                regTodayEmail = todayRegisterEmailCount,
                regAllEmail = totalRegisterEmailCount,
                regTodayFB = todayRegisterFacebookCount,
                regAllFB = totalRegisterFacebookCount
              )
            }
          } else {
            StatsResponse()
          }
        }
      }
    }
}
