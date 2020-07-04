package com.cacoveanu.reader.entity

import javax.persistence.{Column, Entity, Id}

@Entity
class DbBook {

  @Id
  var id: String = _

  @Column(unique = true)
  var path: String = _

  var title: String = _

  var author: String = _

  var collection: String = _

  var mediaType: String = _

  var cover: Array[Byte] = _

  var size: Int = _

  def this(id: String, path: String, title: String, author: String, collection: String, mediaType: String, cover: Array[Byte], size: Int) = {
    this()
    this.id = id
    this.path = path
    this.title = title
    this.author = author
    this.collection = collection
    this.mediaType = mediaType
    this.cover = cover
    this.size = size
  }
}
