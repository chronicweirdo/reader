package com.cacoveanu.reader.service

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import scala.io.StdIn._

object EncryptedPasswordGenerator extends App {

  println("write password:")
  val password = readLine()

  val encoder = new BCryptPasswordEncoder()
  val encodedPassword = encoder.encode(password)

  println(encodedPassword)
}
