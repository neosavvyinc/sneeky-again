package com.phantom.model

import org.joda.time.LocalDate

case class UserRegistration(email : String,
                            birthday : LocalDate,
                            password : String)

case class UserLogin(email : String,
                     password : String)

case class UserResponse(code : Int, message : String) //total dummy class

case class UserInsert(email : String,
                      birthday : LocalDate,
                      saltyHash : String,
                      active : Boolean)

// TO DO
// secret client-facing/obfuscated user id?
case class User(id : Long,
                email : LocalDate,
                birthday : String,
                active : Boolean)
