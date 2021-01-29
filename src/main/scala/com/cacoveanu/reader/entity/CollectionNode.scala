package com.cacoveanu.reader.entity

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import java.util
import scala.beans.BeanProperty

object CollectionNode {
  val EVERYTHING = "everything"
}

class CollectionNode() {

  @BeanProperty
  var name: String = _

  @BeanProperty
  var search: String = _

  @BeanProperty
  var children: java.util.List[CollectionNode] = _

  def this(name: String, search: String) {
    this()
    this.name = name
    this.search = URLEncoder.encode(search, StandardCharsets.UTF_8)
    this.children = new util.ArrayList[CollectionNode]()
  }
}
