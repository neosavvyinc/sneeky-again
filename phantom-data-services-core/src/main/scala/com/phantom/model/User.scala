package com.phantom.model

case class UserRegistration(email : String,
                            birthday : String,
                            password : String)

case class UserLogin(email : String,
                     password : String)

case class UserResponse(code : Int, message : String) //total dummy class

case class UserInsert(email : String,
                      birthday : String,
                      saltyHash : String,
                      active : Boolean)

// TO DO
// secret client-facing/obfuscated user id?
case class User(id : Long,
                email : String,
                birthday : String,
                saltyHash : String,
                active : Boolean)
