package com.cacoveanu.reader.entity

import javax.persistence._

@Entity
class Section {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: java.lang.Long = _

  var index: Int = _

  var link: String = _

  var start: Int = _

  var size: Int = _

  def this(index: Int, link: String, start: Int, size: Int) = {
    this()
    this.index = index
    this.link = link
    this.start = start
    this.size = size
  }
}