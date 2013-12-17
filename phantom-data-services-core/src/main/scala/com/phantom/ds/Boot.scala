package com.phantom.ds

import akka.actor.{ Props, ActorSystem }
import akka.io.IO
import spray.can.Http
import com.phantom.ds.framework.auth.{ PassThroughAuthenticator, SqlSessionRepository, PhantomAuthenticator, MockSessionRepository }

object Boot extends App with DSConfiguration {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")

  // create and start our service actor

  val phantomService = getActor

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(phantomService, interface = "0.0.0.0", port = 9090)

  private def getActor = {
    if (AuthConfiguration.authEnabled) {
      system.actorOf(Props(new PhantomRouteActor() with MockSessionRepository with PhantomAuthenticator), "service")
    } else {
      system.actorOf(Props(new PhantomRouteActor() with PassThroughAuthenticator), "service")
    }
  }
}
