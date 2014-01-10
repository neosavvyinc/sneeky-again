package com.phantom.model

import com.phantom.dataAccess.Profile
import org.joda.time.LocalDate
import java.sql.Date

case class Contact(id : Option[Long],
                   name : String)

trait ContactComponent { this : Profile =>

  import profile.simple._
  import com.github.tototoshi.slick.JodaSupport._

  object ContactTable extends Table[Contact]("CONTACTS") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")

    def * = id.? ~ name <> (Contact, Contact.unapply _)
  }

}
