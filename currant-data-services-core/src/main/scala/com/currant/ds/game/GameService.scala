package com.currant.ds.game

import com.currant.ds.db.{ DB, crud }
import scala.concurrent.{ Future, future }
import com.currant.model.{ Game, GameCreateRequest }
import com.currant.model.GameTypes._
import org.jooq.DSLContext

// for now
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA.
 * User: christopher
 * Date: 11/21/13
 * Time: 8:28 PM
 */

trait GameService {
  def getAll : Future[Seq[Game]]

  def get(id : Long) : Future[Game]

  def create(cr : GameCreateRequest) : Future[Game]

  def update(id : Long, cr : GameCreateRequest) : Future[Game]

  def delete(ids : List[Long]) : Future[Unit]
}

object GameService {
  def apply(db : DB) = new GameService {

    def getAll : Future[Seq[Game]] = {
      future {

        //db.withContext(crud.GameCRUD.list)
        //Serialization.write(Seq(Game(1, "calvinball, no rules", true, None, None, None)))
        Seq(
          Game(1, "calvinball, no rules", true, None, None, None),
          Game(2, "foosball", true, None, None, None)
        )
      }
    }

    def get(id : Long) : Future[Game] = {
      future {
        Game(1, "calvinball, no rules", true, None, None, None)
      }
    }

    def create(cr : GameCreateRequest) : Future[Game] = {
      future {
        Game(1, "calvinball, no rules", true, None, None, None)
      }
    }

    def update(id : Long, cr : GameCreateRequest) : Future[Game] = {
      future {
        Game(1, "calvinball, no rules", true, None, None, None)
      }
    }

    def delete(ids : List[Long]) : Future[Unit] = {
      future {
        Unit
      }
    }
  }
}

