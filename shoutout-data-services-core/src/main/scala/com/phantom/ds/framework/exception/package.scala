package com.phantom.ds.framework

import java.util.ResourceBundle

package object exception {

  class ShoutoutException(val code : Int, val msg : String = "") extends java.lang.Exception(msg)

  class UnverifiedUserException(code : Int, msg : String = "") extends ShoutoutException(code, msg)

  object ShoutoutException {
    //signup/login/auth exceptions will be in the 100 block
    def duplicateUser = new ShoutoutException(101)
    def nonExistentUser = new ShoutoutException(103)
    def unverifiedUser(uuid : String) = new UnverifiedUserException(104, uuid)
    def passwordsComplexity = new ShoutoutException(105)
    def userNotUpdated = new ShoutoutException(106)
    def usernameNotAvailable = new ShoutoutException(107)

    def groupNotFoundException = new ShoutoutException(120)

    def noFeedFound = new ShoutoutException(201)
    def nonExistentConversation = new ShoutoutException(203)
    def unableToUpdateConversationLastChanged = new ShoutoutException(204)

    def contactNotUpdated = new ShoutoutException(301)
    def contactNotInserted = new ShoutoutException(302)
    def nonExistentContact = new ShoutoutException(303)

    def apnsError(msg : String) = new ShoutoutException(400, msg)
  }

  object Errors {

    private val bundle = ResourceBundle.getBundle("errors")

    def getMessage(code : Int) = bundle.getString(code.toString)

  }

}
