package com.phantom.ds

import akka.actor.{ Props, ActorSystem }
import akka.io.IO
import spray.can.Http
import com.phantom.ds.framework.auth._
import com.phantom.ds.integration.twilio.{ TwiioMessageSender, TwilioService, TwilioActor }
import com.phantom.ds.integration.apple.{ AppleActor, AppleService }
import com.phantom.ds.framework.Logging
import java.util.TimeZone
import org.joda.time.DateTimeZone

object Boot extends App with DSConfiguration with Logging {

  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  DateTimeZone.setDefault(DateTimeZone.UTC)

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")

  // create and start our service actor

  val phantomService = getActor
  //println(phantomService.path)
  //println(appleActor.path)
  //println(twilioActor.path)
  println(system)

  implicit val executor = scala.concurrent.ExecutionContext.Implicits.global //TODO <<<---change this to cachedThreadPool possibly

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(phantomService, interface = "0.0.0.0", port = 9090)

  private def getActor = {
    val mode = AuthConfiguration.mode
    log.info(s"---->>>STARTING APPLICATION WITH AUTHENTICATION: $mode <<<------")
    mode match {
      case FullAuthentication       => system.actorOf(Props(new PhantomRouteActor(twilioActor, appleActor) with PhantomRequestAuthenticator with PhantomEntryPointAuthenticator), "service")
      case NonHashingAuthentication => system.actorOf(Props(new PhantomRouteActor(twilioActor, appleActor) with NonHashingRequestAuthenticator with PassThroughEntryPointAuthenticator), "service")
      case NoAuthentication         => system.actorOf(Props(new PhantomRouteActor(twilioActor, appleActor) with PassThroughRequestAuthenticator with PassThroughEntryPointAuthenticator), "service")
    }
  }

  private def twilioActor = system.actorOf(Props(new TwilioActor(twilioService)), "twilio")

  private def appleActor = system.actorOf(Props(new AppleActor()), "apple")

  private def twilioService = TwilioService(TwiioMessageSender(TwilioConfiguration.accountSid, TwilioConfiguration.authToken, TwilioConfiguration.phoneNumber))(executor)
}
