package com.currant.model

/**
 * Created by Neosavvy
 *
 * User: adamparrish
 * Date: 11/16/13
 * Time: 3:08 PM
 */

case class SportCreateRequest(name : String,
                              description : String,
                              active : Boolean,
                              imageUrl : Option[String],
                              minPlayers : Option[Int],
                              maxPlayers : Option[Int],
                              waitList : Option[Int])

case class Sport(id : Long,
                 name : String,
                 description : String,
                 active : Boolean,
                 imageUrl : Option[String],
                 minPlayers : Option[Int],
                 maxPlayers : Option[Int],
                 waitList : Option[Int])

