package com.sneeky.dataAccess

import com.sneeky.ds.SneekyRouteActor
import com.sneeky.ds.framework.{ Dates, Logging }
import com.sneeky.model._

import scala.concurrent.{ ExecutionContext, Future, future }
import scala.slick.jdbc.GetResult
import scala.slick.session.Database

/**
 * Created by aparrish on 8/11/14.
 */
class SneekyDAO(dal : DataAccessLayer, db : Database)(implicit ec : ExecutionContext) extends BaseDAO(dal, db)
    with Logging {

  import dal._
  import dal.profile.simple._

  def insertSneek(shoutout : Sneek)(implicit session : Session) = {
    SneekyTable.forInsert.insert(shoutout)
  }

  private def cleanup(sneekId : Long, userId : Long)(implicit session : Session) = {
    val q1 = for {
      dislikes <- DislikeTable if dislikes.sneekId === sneekId && dislikes.userId === userId
    } yield dislikes
    q1.delete

    val q2 = for {
      likes <- LikeTable if likes.sneekId === sneekId && likes.userId === userId
    } yield likes
    q2.delete
  }

  def unlike(sneekId : Long, userId : Long)(implicit session : Session) : Int = {
    val q2 = for {
      likes <- LikeTable if likes.sneekId === sneekId && likes.userId === userId
    } yield likes
    q2.delete
  }

  def likeSneek(sneekId : Long, userId : Long)(implicit session : Session) : Unit = {
    cleanup(sneekId, userId)
    LikeTable.forInsert.insert(Like(None, sneekId, userId))
  }

  def undislike(sneekId : Long, userId : Long)(implicit session : Session) : Int = {
    val q1 = for {
      dislikes <- DislikeTable if dislikes.sneekId === sneekId && dislikes.userId === userId
    } yield dislikes
    q1.delete
  }

  def dislikeSneek(sneekId : Long, userId : Long)(implicit session : Session) : Unit = {
    cleanup(sneekId, userId)
    DislikeTable.forInsert.insert(Dislike(None, sneekId, userId))
  }

  def findFeedByDate(userId : Long, offset : Long, pageSize : Long)(implicit session : Session) : List[SneekResponse] = {
    import scala.slick.jdbc.{ GetResult, StaticQuery => Q }
    import Q.interpolation

    implicit val getSneekResponse = GetResult(r => SneekResponse(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

    val findFeedByDateQuery =
      sql"""SELECT
              S.ID,
              TEXT,
              IMAGE_URL,
              DATE_FORMAT(CREATED_TIMESTAMP,'%Y-%m-%d %h:%i:%s') AS CREATED_TIMESTAMP,
              (SELECT COUNT(*) FROM DISLIKES WHERE S.ID = SNEEK_ID) AS DISLIKE_COUNT,
              (SELECT COUNT(*) FROM LIKES WHERE S.ID = SNEEK_ID) AS LIKE_COUNT,
              (SELECT COUNT(*) FROM DISLIKES WHERE USER_ID = $userId AND S.ID = SNEEK_ID) AS IS_DISLIKED_BY_ME,
              (SELECT COUNT(*) FROM LIKES WHERE USER_ID = $userId AND S.ID = SNEEK_ID) AS IS_LIKED_BY_ME,
              (SELECT 1 FROM SNEEKS WHERE S.ID = ID AND S.SENDER_ID = $userId) AS IS_OWNED_BY_ME
            FROM
              SNEEKS S LEFT OUTER JOIN DISLIKES D ON S.ID = D.SNEEK_ID
                       LEFT OUTER JOIN LIKES L ON S.ID = L.SNEEK_ID
            GROUP BY
              S.ID
            ORDER BY
              CREATED_TIMESTAMP
            LIMIT $offset,$pageSize
         """.as[SneekResponse]

    findFeedByDateQuery.list

  }

  def findMyFeed(userId : Long, offset : Long, pageSize : Long)(implicit session : Session) : List[SneekResponse] = {
    import scala.slick.jdbc.{ GetResult, StaticQuery => Q }
    import Q.interpolation

    implicit val getSneekResponse = GetResult(r => SneekResponse(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

    val findMyFeedQuery =
      sql"""SELECT
              S.ID,
              TEXT,
              IMAGE_URL,
              DATE_FORMAT(CREATED_TIMESTAMP,'%Y-%m-%d %h:%i:%s') AS CREATED_TIMESTAMP,
              (SELECT COUNT(*) FROM DISLIKES WHERE S.ID = SNEEK_ID) AS DISLIKE_COUNT,
              (SELECT COUNT(*) FROM LIKES WHERE S.ID = SNEEK_ID) AS LIKE_COUNT,
              (SELECT COUNT(*) FROM DISLIKES WHERE USER_ID = $userId AND S.ID = SNEEK_ID) AS IS_DISLIKED_BY_ME,
              (SELECT COUNT(*) FROM LIKES WHERE USER_ID = $userId AND S.ID = SNEEK_ID) AS IS_LIKED_BY_ME,
              (SELECT 1 FROM SNEEKS WHERE S.ID = ID AND S.SENDER_ID = $userId) AS IS_OWNED_BY_ME
            FROM
              SNEEKS S LEFT OUTER JOIN DISLIKES D ON S.ID = D.SNEEK_ID
                       LEFT OUTER JOIN LIKES L ON S.ID = L.SNEEK_ID
            WHERE S.SENDER_ID = $userId
            GROUP BY
              S.ID
            ORDER BY
              CREATED_TIMESTAMP
            LIMIT $offset,$pageSize
         """.as[SneekResponse]

    findMyFeedQuery.list
  }

  def findFeedByPopularity(userId : Long, offset : Long, pageSize : Long)(implicit session : Session) : List[SneekResponse] = {
    import scala.slick.jdbc.{ GetResult, StaticQuery => Q }
    import Q.interpolation

    implicit val getSneekResponse = GetResult(r => SneekResponse(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

    val findFeedByPopularityQuery =
      sql"""SELECT
              S.ID,
              TEXT,
              IMAGE_URL,
              DATE_FORMAT(CREATED_TIMESTAMP,'%Y-%m-%d %h:%i:%s') AS CREATED_TIMESTAMP,
              (SELECT COUNT(*) FROM DISLIKES WHERE S.ID = SNEEK_ID) AS DISLIKE_COUNT,
              (SELECT COUNT(*) FROM LIKES WHERE S.ID = SNEEK_ID) AS LIKE_COUNT,
              (SELECT COUNT(*) FROM DISLIKES WHERE USER_ID = $userId AND S.ID = SNEEK_ID) AS IS_DISLIKED_BY_ME,
              (SELECT COUNT(*) FROM LIKES WHERE USER_ID = $userId AND S.ID = SNEEK_ID) AS IS_LIKED_BY_ME,
              (SELECT 1 FROM SNEEKS WHERE S.ID = ID AND S.SENDER_ID = $userId) AS IS_OWNED_BY_ME
            FROM
              SNEEKS S LEFT OUTER JOIN DISLIKES D ON S.ID = D.SNEEK_ID
                       LEFT OUTER JOIN LIKES L ON S.ID = L.SNEEK_ID
            GROUP BY
              S.ID
            ORDER BY
              (DISLIKE_COUNT + LIKE_COUNT) DESC
            LIMIT $offset,$pageSize
         """.as[SneekResponse]

    findFeedByPopularityQuery.list
  }

}
