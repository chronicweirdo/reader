package com.cacoveanu.reader.entity

case class Resource(url: String, content: Content, prev: Option[String], next: Option[String])
