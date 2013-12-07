package com.currant.ds.db

import java.lang.{ Integer => JInt, Long => JLong }
import java.util
import scala.collection.JavaConversions._
package object crud {

  implicit class OptionIntToJInteger(opt : Option[Int]) {
    def toJInt : JInt = opt.map(x => x : JInt).orNull
  }

  implicit class LongtoJLong(x : Long) {
    def toJLong : JLong = x : JLong
  }

  implicit def listLongstoJlongs(l : List[Long]) : util.Collection[JLong] = {
    l.map(x => x : JLong)
  }
}
