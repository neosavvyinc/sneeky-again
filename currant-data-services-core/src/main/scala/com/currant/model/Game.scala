package com.currant.model

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 11/16/13
 * Time: 3:08 PM
 */

//type GamesList = Seq[Game]

object GameTypes {
  type GamesList = Seq[Game]
}

case class Game(id : Long,
                //name: String,
                description : String,
                active : Boolean,
                imageUrl : Option[String],
                minPlayers : Option[Int],
                maxPlayers : Option[Int])

// waitList: Option[Int]) ??? why Int?

case class GameCreateRequest(
  //name: String,
  description : String,
  active : Boolean,
  imageUrl : Option[String],
  minPlayers : Option[Int],
  maxPlayers : Option[Int])

// waitList: Option[Int]) ??? why Int?
