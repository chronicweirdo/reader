package com.cacoveanu.reader.entity

import java.util.Date
import javax.persistence._

@Entity
class Book {

  @Id
  var id: java.lang.String = _

  @Column(nullable = false)
  var fileType: String = _

  var title: String = _

  var collection: String = _

  var mediaType: String = _

  @Column(length = 10000000) // 10MB
  var cover: Array[Byte] = _

  var size: Int = _

  var added: Date = _

  @OneToMany(fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
  @JoinColumn(name = "book_id")
  var links: java.util.List[BookLink] = _

  @OneToMany(fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
  @JoinColumn(name = "book_id")
  var resources: java.util.List[BookResource] = _

  @OneToMany(fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
  @JoinColumn(name = "book_id")
  var toc: java.util.List[BookTocEntry] = _

  def this(id: String, fileType: String, title: String, collection: String, mediaType: String, cover: Array[Byte], size: Int, added: Date) = {
    this()
    this.id = id
    this.fileType = fileType
    this.title = title
    this.collection = collection
    this.mediaType = mediaType
    this.cover = cover
    this.size = size
    this.added = added
  }
}
