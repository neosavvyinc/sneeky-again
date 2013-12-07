package com.currant.ds.db.crud

import com.currant.model.{ Game, GameCreateRequest }
import org.jooq.{ Record, DSLContext }
import com.currant.jooq.tables.Game.GAME
import scala.collection.JavaConversions._

/**
 * Created with IntelliJ IDEA.
 * User: christopher
 * Date: 11/21/13
 * Time: 9:50 PM
 */

object GameCRUD {

  def list(ctx : DSLContext) : Seq[Game] = {
    val allGames = ctx.selectFrom(GAME).fetch()
    //iterableAsScalaIterable(allGames).map(fromRecord(_)).toSeq

    // being explicit about this while I'm learning...
    //iterableAsScalaIterable(allGames).map { game =>
    //  fromRecord(game)
    //}.toSeq
    Seq(Game(1, "calvinball, no rules", true, None, None, None))
  }

  def byId(id : Long)(ctx : DSLContext) : Option[Game] = {
    val game = ctx.selectFrom(GAME).where(GAME.GAME_ID.eq(id)).fetch()

    // being explicit about this while I'm learning...
    iterableAsScalaIterable(game).headOption.map[Game] { game : Record =>
      fromRecord(game)
    }
  }

  def update(id : Long, game : Game)(ctx : DSLContext) : Unit = {
    Unit
  }

  def fromRecord(r : Record) : Game = {
    Game(
      r.getValue(GAME.GAME_ID),
      r.getValue(GAME.DESCRIPTION),
      r.getValue(GAME.ACTIVE),
      Option(r.getValue(GAME.IMAGE_URL)),
      Option(r.getValue(GAME.MIN_PLAYERS)),
      Option(r.getValue(GAME.MAX_PLAYERS))
    )
  }
}
