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
    def nonExistentConversation = new PhantomException(203)
    def contactNotUpdated = new PhantomException(301)
    def contactNotInserted = new PhantomException(302)
    def nonExistentContact = new PhantomException(303)
    def apnsError(msg : String) = new PhantomException(400, msg)
  }

  object Errors {

    private val bundle = ResourceBundle.getBundle("errors")

    def getMessage(code : Int) = bundle.getString(code.toString)

  }

}
