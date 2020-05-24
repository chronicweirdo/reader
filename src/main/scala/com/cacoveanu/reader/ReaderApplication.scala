package com.cacoveanu.reader

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class ReaderApplication

object ReaderApplication extends App {
  SpringApplication.run(classOf[ReaderApplication])
}