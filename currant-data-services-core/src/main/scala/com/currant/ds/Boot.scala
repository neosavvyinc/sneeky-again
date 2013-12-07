package com.currant.ds

import akka.actor.{ Props, ActorSystem }
import akka.io.IO
import spray.can.Http
import com.currant.ds.db.DB
import com.jolbox.bonecp.{ BoneCP, BoneCPConfig }

object Boot extends App with DSConfiguration {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")

  // create and start our service actor

  val bcpCfg = new BoneCPConfig()
  bcpCfg.setUser(DBConfig.userName)
  bcpCfg.setPassword(DBConfig.password)
  bcpCfg.setJdbcUrl(DBConfig.url)
  bcpCfg.setDefaultAutoCommit(false)
  bcpCfg.setDisableJMX(true)

  val bcp = new BoneCP(bcpCfg)

  val db = DB(bcp)

  val currantService = system.actorOf(Props(new CurrantRouteActor(db)), "service")

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(currantService, interface = "localhost", port = 9090)
}