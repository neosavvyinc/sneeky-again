package com.phantom.ds.framework

import spray.httpx.marshalling.{ ToResponseMarshallingContext, ToResponseMarshaller }
import scala.concurrent.{ ExecutionContext, Future }
import spray.http.ContentTypes._
import spray.http.HttpEntity
import spray.http.StatusCodes.OK
import spray.json._
import com.phantom.model._
import com.phantom.ds.framework.exception.{ UnverifiedUserException, PhantomException }
import spray.http.HttpResponse

import com.phantom.model.ConversationItem
import java.util.UUID

import com.phantom.model.UserRegistration

import org.joda.time.{ LocalDate, DateTimeZone, DateTime }
import org.joda.time.format.ISODateTimeFormat
import com.phantom.ds.integration.twilio.InviteMessageStatus

package object httpx {

  private[httpx]type JF[T] = JsonFormat[T]

  trait PhantomJsonProtocol extends DefaultJsonProtocol with Logging {

    implicit object JodaDateTimeFormat extends JsonFormat[DateTime] {

      val formatter = ISODateTimeFormat.basicDateTimeNoMillis

      def write(obj : DateTime) : JsValue = JsString(formatter.print(obj.toDateTime(DateTimeZone.UTC)))

      def read(json : JsValue) : DateTime = json match {
        case JsString(x) => formatter.parseDateTime(x).toDateTime(DateTimeZone.UTC)
        case _           => deserializationError("Expected String value for DateTime")
      }
    }

    implicit object JodaLocalDateFormat extends JsonFormat[LocalDate] {

      val formatter = ISODateTimeFormat.basicDate()

      def write(obj : LocalDate) : JsValue = JsString(formatter.print(obj))

      def read(json : JsValue) : LocalDate = json match {
        case JsString(x) => formatter.parseLocalDate(x)
        case _           => deserializationError("Expected String value for LocalDate")
      }
    }

    implicit object UserStatusFormat extends JsonFormat[UserStatus] {
      def write(obj : UserStatus) = JsString(UserStatus.toStringRep(obj))

      def read(json : JsValue) : UserStatus = json match {
        case JsString(x) => UserStatus.fromStringRep(x)
        case _           => deserializationError("Expected String value for UserStatus")
      }
    }

    implicit object PushSettingTypeFormat extends JsonFormat[PushSettingType] {
      def write(obj : PushSettingType) = JsString(PushSettingType.toStringRep(obj))

      def read(json : JsValue) : PushSettingType = json match {
        case JsString(x) => PushSettingType.fromStringRep(x)
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

    implicit object ListTypeFormat extends JsonFormat[List[ConversationItem]] {
      override def write(obj : List[ConversationItem]) : JsValue = JsArray(obj.map(conversationItemFormat.write))

      override def read(json : JsValue) : List[ConversationItem] = json match {
        case JsArray(x) => x.map(conversationItemFormat.read)
        case _          => deserializationError("Expected String value for List[ConversationItem]")
      }
    }

    implicit val failureFormat = jsonFormat2(Failure)
    implicit val userRegistrationFormat = jsonFormat3(UserRegistration)
    implicit val userRegistrationResponseFormat = jsonFormat2(RegistrationResponse)

    implicit val userFormat = jsonFormat11(PhantomUser)
    implicit val userLoginFormat = jsonFormat2(UserLogin)
    implicit val loginSuccessFormat = jsonFormat1(LoginSuccess)
    implicit val registrationVerificationFormat = jsonFormat6(RegistrationVerification)
    implicit val inviteMessageStatusFormat = jsonFormat2(InviteMessageStatus)
    implicit val sessionIdwithPushNotifier = jsonFormat2(UpdatePushTokenRequest)
    implicit val pushSettingsRequest = jsonFormat2(PushSettingsRequest)
    implicit val conversationFormat = jsonFormat5(Conversation)
    implicit val conversationItemFormat = jsonFormat10(ConversationItem)

    implicit val feedEntryRequest = jsonFormat2(FeedEntry)
    implicit val contactFormat = jsonFormat4(Contact)

    implicit val conversationInsertResponse = jsonFormat1(ConversationInsertResponse)
    implicit val conversationUpdateResponse = jsonFormat1(ConversationUpdateResponse)
    implicit val blockUserByConversationResponse = jsonFormat2(BlockUserByConversationResponse)

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
        case x : PhantomException        => Failure(x.code, Errors.getMessage(x.code))
        case x                           => log.error(x.getMessage, x); Failure(defaultCode, Errors.getMessage(defaultCode))
      }
      failureFormat.write(failure)
    }
  }

  case class Failure(errorCode : Int, displayError : String)

}
