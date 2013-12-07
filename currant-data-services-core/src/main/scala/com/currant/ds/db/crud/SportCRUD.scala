package com.currant.ds.db.crud

import com.currant.model.{ Sport, SportCreateRequest }
import org.jooq.{ Record, DSLContext }
import com.currant.jooq.tables.Sport.SPORT
import scala.collection.JavaConversions._
import com.currant.ds.framework.Logging

object SportCRUD extends Logging {

  def create(cr : SportCreateRequest)(ctx : DSLContext) = {
    ctx.insertInto(SPORT,
      SPORT.LABEL,
      SPORT.DESCRIPTION,
      SPORT.ACTIVE,
      SPORT.IMAGE_URL,
      SPORT.MAX_PLAYERS,
      SPORT.MIN_PLAYERS,
      SPORT.WAIT_LIST_AMOUNT)
      .values(
        cr.name,
        cr.description,
        cr.active,
        cr.imageUrl.orNull,
        cr.maxPlayers.toJInt,
        cr.minPlayers.toJInt,
        cr.waitList.toJInt)
      .returning(SPORT.SPORT_ID)
      .fetchOne().getSportId

  }

  def update(sport : Sport)(ctx : DSLContext) = {
    ctx.update(SPORT)
      .set(SPORT.LABEL, sport.name)
      .set(SPORT.DESCRIPTION, sport.description)
      .set(SPORT.ACTIVE, sport.active : java.lang.Boolean) //weird
      .set(SPORT.IMAGE_URL, sport.imageUrl.orNull)
      .set(SPORT.MAX_PLAYERS, sport.maxPlayers.toJInt)
      .set(SPORT.MIN_PLAYERS, sport.minPlayers.toJInt)
      .set(SPORT.WAIT_LIST_AMOUNT, sport.waitList.toJInt)
      .where(SPORT.SPORT_ID.eq(sport.id))
  }

  def list(ctx : DSLContext) : Seq[Sport] = {
    val bla = ctx.selectFrom(SPORT).fetch()
    iterableAsScalaIterable(bla).map(fromRecord(_)).toSeq
  }

  def byId(id : Long)(ctx : DSLContext) : Option[Sport] = {
    val bla = ctx.selectFrom(SPORT).where(SPORT.SPORT_ID.eq(id)).fetch()
    iterableAsScalaIterable(bla).headOption.map(fromRecord(_))
  }

  def delete(ids : List[Long])(ctx : DSLContext) = {
    ctx.delete(SPORT).where(SPORT.SPORT_ID.in(ids)).execute()
  }

  def fromRecord(r : Record) : Sport = {
    debug(r.toString)
    Sport(r.getValue(SPORT.SPORT_ID),
      r.getValue(SPORT.LABEL),
      r.getValue(SPORT.DESCRIPTION),
      r.getValue(SPORT.ACTIVE),
      Option(r.getValue(SPORT.IMAGE_URL)),
      Option(r.getValue(SPORT.MIN_PLAYERS)).map(_.toInt),
      Option(r.getValue(SPORT.MAX_PLAYERS)).map(_.toInt),
      Option(r.getValue(SPORT.WAIT_LIST_AMOUNT)).map(_.toInt))
  }
}