package com.phantom.ds

import akka.actor.{ Props, ActorSystem }
import akka.io.IO
import spray.can.Http
import com.phantom.ds.framework.auth._
import com.phantom.ds.integration.apple.{ AppleActor, AppleService }
import com.phantom.ds.framework.Logging
import java.util.TimeZone
import org.joda.time.DateTimeZone
import com.phantom.ds.integration.amazon.S3Service

object Boot extends App with DSConfiguration with Logging {

  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  DateTimeZone.setDefault(DateTimeZone.UTC)

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")

  val phantomService = getActor

  implicit val executor = scala.concurrent.ExecutionContext.Implicits.global

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(phantomService, interface = "0.0.0.0", port = 9090)

  private def getActor = {
    val mode = AuthConfiguration.mode
    log.info(s"---->>>STARTING APPLICATION WITH AUTHENTICATION: $mode <<<------")
    mode match {
      case FullAuthentication       => system.actorOf(Props(new PhantomRouteActor(appleActor, S3Service()) with PhantomRequestAuthenticator with PhantomEntryPointAuthenticator), "service")
      case NonHashingAuthentication => system.actorOf(Props(new PhantomRouteActor(appleActor, S3Service()) with NonHashingRequestAuthenticator with PassThroughEntryPointAuthenticator), "service")
      case NoAuthentication         => system.actorOf(Props(new PhantomRouteActor(appleActor, S3Service()) with PassThroughRequestAuthenticator with PassThroughEntryPointAuthenticator), "service")
      case DebugAuthentication      => system.actorOf(Props(new PhantomRouteActor(appleActor, S3Service()) with DebugAuthenticator with PhantomEntryPointAuthenticator), "service")
    }
  }

  private def appleActor = system.actorOf(Props(new AppleActor()), "apple")

}
