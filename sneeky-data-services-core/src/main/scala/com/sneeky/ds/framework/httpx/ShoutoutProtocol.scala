package com.sneeky.ds.framework

import spray.httpx.marshalling.{ ToResponseMarshallingContext, ToResponseMarshaller }
import scala.concurrent.{ ExecutionContext, Future }
import spray.http.ContentTypes._
import spray.http.HttpEntity
import spray.http.StatusCodes.OK
import spray.json._
import com.sneeky.model._
import com.sneeky.ds.framework.exception.{ UnverifiedUserException, ShoutoutException }
import spray.http.HttpResponse

import java.util.UUID

import org.joda.time.{ LocalDate, DateTime }

package object httpx {

  private[httpx]type JF[T] = JsonFormat[T]

  trait SneekyJsonProtocol extends DefaultJsonProtocol with Logging {

    implicit object JodaDateTimeFormat extends JsonFormat[DateTime] {

      def write(obj : DateTime) : JsValue = JsString(Dates.write(obj))

      def read(json : JsValue) : DateTime = json match {
        case JsString(x) => Dates.readDateTime(x)
        case _           => deserializationError("Expected String value for DateTime")
      }
    }

    implicit object JodaLocalDateFormat extends JsonFormat[LocalDate] {

      def write(obj : LocalDate) : JsValue = JsString(Dates.write(obj))

      def read(json : JsValue) : LocalDate = json match {
        case JsString(x) => Dates.readLocalDate(x)
        case _           => deserializationError("Expected String value for LocalDate")
      }
    }

    implicit object PushSettingTypeFormat extends JsonFormat[SettingType] {
      def write(obj : SettingType) = JsString(SettingType.toStringRep(obj))

      def read(json : JsValue) : SettingType = json match {
        case JsString(x) => SettingType.fromStringRep(x)
        case _           => deserializationError("Expected String value for PushSettingType")
      }
    }

    implicit object MobilePushTypeFormat extends JsonFormat[MobilePushType] {
      def write(obj : MobilePushType) = JsString(MobilePushType.toStringRep(obj))

      def read(json : JsValue) : MobilePushType = json match {
        case JsString(x) => MobilePushType.fromStringRep(x)
        case _           => deserializationError("Expected String value for MobilePushType")
      }
    }

    implicit object UUIDFormat extends JsonFormat[UUID] {
      def write(obj : UUID) = JsString(UUIDConversions.toStringRep(obj))

      def read(json : JsValue) : UUID = json match {
        case JsString(x) => UUIDConversions.fromStringRep(x)
        case _           => deserializationError("Expected String value for UUID")
      }
    }

    implicit object UserStatusFormat extends JsonFormat[UserStatus] {
      def write(obj : UserStatus) = JsString(UserStatus.toStringRep(obj))

      def read(json : JsValue) : UserStatus = json match {
        case JsString(x) => UserStatus.fromStringRep(x)
        case _           => deserializationError("Expected String value for UserStatus")
      }
    }

    implicit object ShoutoutResponseListTypeFormat extends JF[List[ShoutoutResponse]] {
      override def write(obj : List[ShoutoutResponse]) : JsValue = JsArray(obj.map(shoutoutResponse2json.write))

      override def read(json : JsValue) : List[ShoutoutResponse] = json match {
        case JsArray(x) => x.map(shoutoutResponse2json.read)
        case _          => deserializationError("Expected String value for List[ShoutoutResponse]")
      }
    }

    implicit val failureFormat = jsonFormat2(Failure)

    implicit val shoutuser2json = jsonFormat6(SneekyV2User)
    implicit val activeUser2json = jsonFormat9(ActiveSneekyV2User)

    implicit val shoutout2json = jsonFormat11(Shoutout)
    implicit val shoutoutResponse2json = jsonFormat5(ShoutoutResponse)
    implicit val updatePushToken2json = jsonFormat2(UpdatePushTokenRequest)

    implicit val settingsRequest2json = jsonFormat2(SettingsRequest)

    implicit val statsRequest2json = jsonFormat3(StatsRequest)
    implicit val statsResponse2json = jsonFormat6(StatsResponse)
  }

  trait PhantomResponseMarshaller extends SneekyJsonProtocol {

    implicit def phantomResponse[T](implicit ec : ExecutionContext, format : JF[T]) = new PhantomResponse[T]
  }

  class PhantomResponse[T](implicit ec : ExecutionContext, format : JF[T]) extends ToResponseMarshaller[Future[T]] with SneekyJsonProtocol {

    import com.sneeky.ds.framework.exception.Errors
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
        case x : UnverifiedUserException => Failure(x.code, x.msg)
        case x : ShoutoutException       => Failure(x.code, Errors.getMessage(x.code))
        case x                           => log.error(x.getMessage, x); Failure(defaultCode, Errors.getMessage(defaultCode))
      }
      failureFormat.write(failure)
    }
  }

  case class Failure(errorCode : Int, displayError : String)

}
