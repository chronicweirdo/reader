package com.cacoveanu.reader.entity

case class Content(index: Option[Int], mediaType: String, data: Array[Byte], meta: Map[String, Any] = Map())
