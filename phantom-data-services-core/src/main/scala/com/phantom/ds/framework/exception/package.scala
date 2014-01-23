package com.phantom.ds.framework

import java.util.ResourceBundle

package object exception {

  class PhantomException(val code : Int, val msg : String = "") extends java.lang.Exception(msg)

  class UnverifiedUserException(code : Int, msg : String = "") extends PhantomException(code, msg)

  object PhantomException {
    //signup/login/auth exceptions will be in the 100 block
    def duplicateUser = new PhantomException(101)
    def nonExistentUser = new PhantomException(103)
    def unverifiedUser(uuid : String) = new UnverifiedUserException(104, uuid)
    def passwordsComplexity = new PhantomException(105)
    def noFeedFound = new PhantomException(201)
    def contactNotUpdated = new PhantomException(202)

  }

  object Errors {

    private val bundle = ResourceBundle.getBundle("errors")

    def getMessage(code : Int) = bundle.getString(code.toString)

  }

}
