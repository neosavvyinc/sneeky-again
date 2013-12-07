package com.phantom.ds.framework

import java.util.ResourceBundle

package object exception {

  trait PhantomException {
    def code : Int
    def message : String
  }

  object Errors {

    private val bundle = ResourceBundle.getBundle("errors")

    def getMessage(code : Int) = bundle.getString(code.toString)

  }

}