package com.currant.ds.user

import com.currant.jooq.tables.CurrantUser.CURRANT_USER
import com.currant.jooq.tables.Profile.PROFILE
import com.currant.jooq.tables.ProfileSport.PROFILE_SPORT
import com.currant.model.{ ProfileInsert, CurrantUserInsert }
import org.jooq.DSLContext

object UserCRUD {

  def emailCheck(email : String)(implicit ctx : DSLContext) = {
    ctx.selectOne().from(CURRANT_USER).where(CURRANT_USER.EMAIL_ADDRESS.eq(email))
  }

  def insertUser(req : CurrantUserInsert)(implicit ctx : DSLContext) = {
    ctx.insertInto(CURRANT_USER,
      CURRANT_USER.EMAIL_ADDRESS,
      CURRANT_USER.PASSWORD,
      CURRANT_USER.ACCOUNT_STATUS,
      CURRANT_USER.SUBSCRIBER_TYPE,
      CURRANT_USER.ACTIVE).
      values(
        req.email,
        req.saltyHash,
        req.accountStatus.toString,
        req.subscriberType,
        req.active
      ).returning(CURRANT_USER.CURRANT_USER_ID).fetchOne().getCurrantUserId
  }

  def insertProfile(req : ProfileInsert)(implicit ctx : DSLContext) = {
    ctx.insertInto(PROFILE,
      PROFILE.CURRANT_USER_ID,
      PROFILE.SOURCE,
      PROFILE.SOURCE_IDENTIFIER,
      PROFILE.FIRST_NAME,
      PROFILE.LAST_NAME,
      PROFILE.PROFILE_LEVEL,
      PROFILE.PREFERRED_TIME).
      values(
        req.userId,
        req.source.toString,
        req.sourceIdentifier,
        req.firstName,
        req.lastName,
        req.profileLevel.toString,
        req.preferredTime
      ).returning(PROFILE.PROFILE_ID).fetchOne().getProfileId
  }

  def deleteFavoriteSports(profileId : Long)(ctx : DSLContext) = {
    ctx.delete(PROFILE_SPORT).where(PROFILE_SPORT.PROFILE_ID.eq(profileId))
  }

  def insertFavoriteSports(profileId : Long, sports : Seq[Long])(implicit ctx : DSLContext) = {
    val q = sports.zipWithIndex
    val queries = q.map(x => insertFavoriteSportQuery(profileId, x._1, x._2, ctx))
    ctx.batch(queries : _*).execute()
  }

  private def insertFavoriteSportQuery(profileId : Long, sportId : Long, order : Int, ctx : DSLContext) = {
    ctx.insertInto(PROFILE_SPORT, PROFILE_SPORT.PROFILE_ID, PROFILE_SPORT.SPORT_ID, PROFILE_SPORT.SORT_ORDER).values(profileId, sportId, order)
  }

}