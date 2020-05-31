package com.cacoveanu.reader.service

import org.junit.jupiter.api.Test

import scala.util.matching.Regex

class SearchTermProcessing {

  @Test
  def stripSensitiveCharacters(): Unit = {
    val original = "Marvel Comics -  Infinity Sagas (Guantlet, War, Crusade, Abyss & The End) - Complete\\01 - Thanos Quest"
    val lowercase = original.toLowerCase()

    val pattern = "[A-Za-z0-9]+".r
    val matches: Regex.MatchIterator = pattern.findAllIn(lowercase)
    val result = "%" + matches.mkString("%") + "%"
    println(result)
  }
}
