package com.cacoveanu.reader.service

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import scala.io.StdIn._

object EncryptedPasswordGenerator extends App {

  val password = readLine()

  val encoder = new BCryptPasswordEncoder()
  val encodedPassword = encoder.encode(password)

  println(encodedPassword)
}
