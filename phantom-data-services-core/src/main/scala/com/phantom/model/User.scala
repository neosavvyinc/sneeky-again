package com.phantom.model

import org.joda.time.LocalDate

case class UserRegistration(email : String,
                            birthday : LocalDate,
                            password : String)

case class UserLogin(email : String,
                     password : String)

case class ClientSafeUserResponse(email : String,
                                  phoneNumber : String,
                                  birthday : LocalDate,
                                  newPictureReceivedNotification : Boolean,
                                  soundsNotification : Boolean)

case class PhantomUser(id : String)

case class UserInsert(email : String,
                      birthday : LocalDate,
                      saltyHash : String,
                      active : Boolean)

// TO DO
// secret client-facing/obfuscated user id?
case class User(id : Long,
                email : String,
                birthday : LocalDate,
                active : Boolean)
