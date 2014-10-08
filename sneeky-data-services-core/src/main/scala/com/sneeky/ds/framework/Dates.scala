package com.sneeky.ds.framework

import org.joda.time.{ DateTimeZone, DateTime, LocalDate }
import org.joda.time.format.{ ISODateTimeFormat, DateTimeFormat }

object Dates {

  private val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss")

  private val localDateFormat = ISODateTimeFormat.basicDate()

  def readDateTime(str : String) : DateTime = dateTimeFormat.parseDateTime(str)

  def write(dateTime : DateTime) : String = dateTimeFormat.print(dateTime)

  def readLocalDate(str : String) : LocalDate = localDateFormat.parseLocalDate(str)

  def write(localDate : LocalDate) : String = localDateFormat.print(localDate)

  def nowDTStr : String = write(nowDT)

  def nowDT : DateTime = DateTime.now(DateTimeZone.UTC)

  def nowLD : LocalDate = LocalDate.now(DateTimeZone.UTC)

  def nowLDStr : String = write(nowLD)

}