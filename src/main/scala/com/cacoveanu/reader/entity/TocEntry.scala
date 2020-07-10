package com.cacoveanu.reader.entity

class TocEntry {

  var index: Int = _

  var title: String = _

  var link: String = _

  def this(index: Int, title: String, link: String) = {
    this()
    this.index = index
    this.title = title
    this.link = link
  }
}