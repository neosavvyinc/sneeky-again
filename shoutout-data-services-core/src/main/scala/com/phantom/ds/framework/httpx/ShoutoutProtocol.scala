package com.phantom.ds.framework

import spray.httpx.marshalling.{ ToResponseMarshallingContext, ToResponseMarshaller }
import scala.concurrent.{ ExecutionContext, Future }
import spray.http.ContentTypes._
import spray.http.HttpEntity
import spray.http.StatusCodes.OK
import spray.json._
import com.phantom.model._
import com.phantom.ds.framework.exception.{ UnverifiedUserException, ShoutoutException }
import spray.http.HttpResponse

import java.util.UUID

import org.joda.time.{ LocalDate, DateTime }

package object httpx {

  private[httpx]type JF[T] = JsonFormat[T]

  trait PhantomJsonProtocol extends DefaultJsonProtocol with Logging {

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

    implicit object ContactTypeFormat extends JsonFormat[ContactType] {
      override def write(obj : ContactType) : JsValue = JsString(ContactType.toStringRep(obj))

      override def read(json : JsValue) : ContactType = json match {
        case JsString(x) => ContactType.fromStringRep(x)
        case _           => deserializationError("Expected String value for ContactType")
      }
    }

    implicit object GroupListTypeFormat extends JF[List[Group]] {
      override def write(obj : List[Group]) : JsValue = JsArray(obj.map(group2json.write))

      override def read(json : JsValue) : List[Group] = json match {
        case JsArray(x) => x.map(group2json.read)
        case _          => deserializationError("Expected String value for List[Group]")
      }
    }

    implicit val failureFormat = jsonFormat2(Failure)

    implicit val shoutuser2json = jsonFormat11(ShoutoutUser)
    implicit val loginWithEmail2json = jsonFormat2(UserLogin)
    implicit val loginWithFacebook2json = jsonFormat4(FacebookUserLogin)
    implicit val loginSuccess2json = jsonFormat1(LoginSuccess)
    implicit val userRegistration2json = jsonFormat2(UserRegistrationRequest)
    implicit val registrationResponse2json = jsonFormat1(RegistrationResponse)
    implicit val userUpdateRequest2json = jsonFormat4(ShoutoutUserUpdateRequest)
    implicit val activeUser2json = jsonFormat6(ActiveShoutoutUser)

    implicit val contact2json = jsonFormat6(Contact)
    implicit val contactOrdering2json = jsonFormat3(ContactOrdering)
    implicit val contactRequest2json = jsonFormat1(ContactsRequest)
    implicit val friend2json = jsonFormat4(Friend)
    implicit val group2json = jsonFormat3(Group)
    implicit val aggregateContact2json = jsonFormat5(AggregateContact)

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
        case x : UnverifiedUserException => Failure(x.code, x.msg)
        case x : ShoutoutException       => Failure(x.code, Errors.getMessage(x.code))
        case x                           => log.error(x.getMessage, x); Failure(defaultCode, Errors.getMessage(defaultCode))
      }
      failureFormat.write(failure)
    }
  }

  case class Failure(errorCode : Int, displayError : String)

}
