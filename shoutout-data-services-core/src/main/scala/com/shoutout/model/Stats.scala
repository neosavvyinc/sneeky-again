package com.shoutout.model

import org.joda.time.LocalDate

/**
 * Created by Dave on 8/25/14.
 */

case class StatsRequest(password : String,
                        date : Option[LocalDate],
                        timezone : Option[String])

case class StatsResponse(sentToday : Option[Int] = None,
                         sentAll : Option[Int] = None,
                         regTodayEmail : Option[Int] = None,
                         regTodayFB : Option[Int] = None,
                         regAllEmail : Option[Int] = None,
                         regAllFB : Option[Int] = None)