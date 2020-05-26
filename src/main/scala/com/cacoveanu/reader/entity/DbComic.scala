package com.cacoveanu.reader.entity

import com.cacoveanu.reader.service.ComicPage
import javax.persistence.{Column, Entity, GeneratedValue, GenerationType, Id, Transient}
import org.springframework.http.MediaType

@Entity
class DbComic {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: java.lang.Long = _

  @Column(unique = true)
  var path: String = _

  var title: String = _

  var collection: String = _

  var mediaType: MediaType = _

  var cover: Array[Byte] = _

  @Transient
  var pages: Seq[ComicPage] = _

  def this(path: String, title: String, collection: String, mediaType: MediaType, cover: Array[Byte]) {
    this()
    this.path = path
    this.title = title
    this.collection = collection
    this.mediaType = mediaType
    this.cover = cover
  }
}
