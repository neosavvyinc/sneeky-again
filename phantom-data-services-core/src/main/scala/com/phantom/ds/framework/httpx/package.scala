package com.phantom.ds.framework

import spray.httpx.marshalling.{ ToResponseMarshallingContext, ToResponseMarshaller }
import scala.concurrent.{ ExecutionContext, Future }
import spray.http.ContentTypes._
import spray.http.{ HttpEntity, HttpResponse }
import spray.http.StatusCodes.OK
import spray.json._
import com.phantom.model.{ UserResponse, User, UserInsert, UserRegistration }
import com.phantom.ds.framework.exception.PhantomException

package object httpx {

  private[httpx]type JF[T] = JsonFormat[T]

  trait PhantomJsonProtocol extends DefaultJsonProtocol {
    implicit val failureFormat = jsonFormat3(Failure)
    implicit val userRegistrationFormat = jsonFormat3(UserRegistration)
    implicit val userInsertFormat = jsonFormat4(UserInsert)
    implicit val userFormat = jsonFormat5(User)
    implicit val regResponse = jsonFormat2(UserResponse)
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
        case x : PhantomException => Failure(x.code, x.message, Errors.getMessage(x.code))
        case _                    => Failure(defaultCode, t.getMessage, Errors.getMessage(defaultCode))
      }
      failureFormat.write(failure)
    }
  }

  case class Failure(errorCode : Int, errorMessage : String, displayError : String)

  //case class Payload[T](payload : T)
}
