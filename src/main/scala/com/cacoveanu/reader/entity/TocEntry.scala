package com.cacoveanu.reader.entity

import javax.persistence._

@Entity
class TocEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: java.lang.Long = _

  var index: Int = _

  var title: String = _

  var link: String = _

  var start: Int = _

  var size: Int = _

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "book_id", nullable = false)
  var book: Book = _

  def this(index: Int, title: String, link: String, start: Int, size: Int) = {
    this()
    this.index = index
    this.title = title
    this.link = link
    this.start = start
    this.size = size
  }
}