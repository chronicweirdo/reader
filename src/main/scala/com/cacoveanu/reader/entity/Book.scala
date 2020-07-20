package com.cacoveanu.reader.entity

import com.cacoveanu.reader.util.EpubUtil

import scala.jdk.CollectionConverters._
import javax.persistence.{CascadeType, Column, Entity, FetchType, GeneratedValue, GenerationType, Id, JoinColumn, OneToMany, Transient}

@Entity
class Book {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: java.lang.Long = _

  @Column(unique = true)
  var path: String = _

  var title: String = _

  var author: String = _

  var collection: String = _

  var mediaType: String = _

  @Column(length = 10000000) // 10MB
  var cover: Array[Byte] = _

  var size: Int = _

  var tocLink: String = _

  @OneToMany(fetch = FetchType.EAGER, cascade = Array(CascadeType.ALL))
  @JoinColumn(name = "book_id")
  var toc: java.util.List[TocEntry] = _

  @Transient
  def getSections(): Seq[TocEntry] = EpubUtil.getSections(toc.asScala.toSeq)

  def this(path: String, title: String, author: String, collection: String, mediaType: String, cover: Array[Byte], size: Int) = {
    this()
    this.path = path
    this.title = title
    this.author = author
    this.collection = collection
    this.mediaType = mediaType
    this.cover = cover
    this.size = size
  }
}
