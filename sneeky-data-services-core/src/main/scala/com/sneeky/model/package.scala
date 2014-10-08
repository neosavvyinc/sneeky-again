package com.sneeky

package object model {

  sealed trait Paging

  object Paging {
    def apply(page : Int, size : Int) = {
      if (page < 1 || size < 0) {
        NoPaging
      } else {
        PageRequest(page, size)
      }
    }
  }

  case object NoPaging extends Paging

  case class PageRequest(page : Int, size : Int) extends Paging

}
