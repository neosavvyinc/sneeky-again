package com.currant.model

import spray.json._

//order matters
object CurrantUserJsonImplicits extends DefaultJsonProtocol {

  implicit val colorFormat = jsonFormat7(CurrantUserRegistration)
  implicit val insertFormat = jsonFormat2(InsertResponse)

  implicit object ProfileSourceFormat extends RootJsonFormat[ProfileSource] {

    val facebook = JsString("facebook")
    val currant = JsString("currant")

    def write(obj : ProfileSource) : JsValue = obj match {
      case Facebook => facebook
      case Currant  => currant
    }

    def read(json : JsValue) : ProfileSource = json match {
      case `facebook` => Facebook
      case `currant`  => Currant
      case _          => deserializationError(s"json $json is not a valid ProfileSource")
    }
  }

  implicit object ProfileLevelFormat extends RootJsonFormat[ProfileLevel] {

    val standard = JsString("standard")
    val elite = JsString("elite")

    def write(obj : ProfileLevel) : JsValue = obj match {
      case Standard => standard
      case Elite    => elite
    }

    def read(json : JsValue) : ProfileLevel = json match {
      case `standard` => Standard
      case `elite`    => Elite
      case _          => deserializationError(s" json $json is not a valid ProfileLevel")
    }
  }

  implicit object ProfileTimeFormat extends RootJsonFormat[ProfileTime] {

    val earlyMorning = JsString("earlyMorning")
    val morning = JsString("morning")
    val day = JsString("day")
    val evening = JsString("evening")
    val night = JsString("night")

    def write(obj : ProfileTime) : JsValue = obj match {
      case EarlyMorning => earlyMorning
      case Morning      => morning
      case Day          => day
      case Evening      => evening
      case Night        => night
    }

    def read(json : JsValue) : ProfileTime = json match {
      case `earlyMorning` => EarlyMorning
      case `morning`      => Morning
      case `day`          => Day
      case `evening`      => Evening
      case `night`        => Night
      case _              => deserializationError(s"json $json is not a valid ProfileTime")
    }
  }

  implicit val profileFormat = jsonFormat17(Profile)

}