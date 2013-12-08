package com.phantom.ds.framework

import spray.httpx.marshalling.{ ToResponseMarshallingContext, ToResponseMarshaller }
import scala.concurrent.{ ExecutionContext, Future }
import spray.http.ContentTypes._
import spray.http.{ HttpEntity, HttpResponse }
import spray.http.StatusCodes.OK
import spray.json._
import com.phantom.model.{ UserResponse, User, UserInsert, UserRegistration }
import com.phantom.model._
import com.phantom.ds.framework.exception.PhantomException
import spray.http.HttpResponse
import spray.http.HttpResponse
import spray.http.HttpResponse
import spray.http.HttpResponse
import com.phantom.model.UserInsert
import spray.http.HttpResponse
import com.phantom.model.ConversationSummary
import com.phantom.model.ConversationItem
import com.phantom.model.User
import com.phantom.model.ConversationStarter
import com.phantom.model.UserRegistration
import com.phantom.model.ConversationDetail

package object httpx {

  private[httpx]type JF[T] = JsonFormat[T]

  trait PhantomJsonProtocol extends DefaultJsonProtocol {
    implicit val failureFormat = jsonFormat2(Failure)
    implicit val userRegistrationFormat = jsonFormat3(UserRegistration)
    implicit val userInsertFormat = jsonFormat4(UserInsert)
    implicit val userFormat = jsonFormat5(User)
    implicit val userResponse = jsonFormat2(UserResponse)

    implicit val conversationStarterFormat = jsonFormat4(ConversationStarter)
    implicit val conversationItemFormat = jsonFormat6(ConversationItem)
    implicit val conversationSummaryFormat = jsonFormat1(ConversationSummary)
    implicit val conversationDetail = jsonFormat2(ConversationDetail)
    implicit val conversationFeed = jsonFormat1(Feed)

    //implicit def payloadFormat[T](implicit tag : ClassManifest[T]) : RootJsonFormat[Payload[T]] = jsonFormat1(Payload[T])
  }

  trait PhantomResponseMarshaller extends PhantomJsonProtocol {

    implicit def phantomResponse[T](implicit ec : ExecutionContext, format : JF[T]) = new PhantomResponse[T]
  }

  class PhantomResponse[T](implicit ec : ExecutionContext, format : JF[T]) extends ToResponseMarshaller[Future[T]] with PhantomJsonProtocol {

    import com.phantom.ds.framework.exception.Errors

    private def payload = "payload"
    private def defaultCode = 500

    def apply(value : Future[T], ctx : ToResponseMarshallingContext) : Unit = {

      value.onSuccess {
        case result => ctx.marshalTo(HttpResponse(OK, HttpEntity(`application/json`, toJsonPayload(result, format).compactPrint)))
      }

      value.onFailure {
        case throwable : Throwable => ctx.marshalTo(HttpResponse(OK, HttpEntity(`application/json`, toJson(throwable).compactPrint)))
      }
    }

    private def toJsonPayload(result : T, format : JF[T]) = JsObject(payload -> format.write(result))

    private def toJson(t : Throwable) = {
      val failure = t match {
        case x : PhantomException => Failure(x.code, Errors.getMessage(x.code))
        case _                    => Failure(defaultCode, Errors.getMessage(defaultCode))
      }
      failureFormat.write(failure)
    }
  }

  case class Failure(errorCode : Int, displayError : String)

  //case class Payload[T](payload : T)
}
