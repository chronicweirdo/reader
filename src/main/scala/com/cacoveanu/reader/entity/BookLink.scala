package com.cacoveanu.reader.entity

import javax.persistence.{Entity, GeneratedValue, GenerationType, Id}

@Entity
class BookLink {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: java.lang.Long = _

  //var bookId: java.lang.Long = _

  var link: String = _

  var position: java.lang.Long = _
}
