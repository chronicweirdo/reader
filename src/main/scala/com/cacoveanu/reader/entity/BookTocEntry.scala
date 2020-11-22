package com.cacoveanu.reader.entity

import javax.persistence.{Entity, GeneratedValue, GenerationType, Id}

@Entity
class BookTocEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: java.lang.Long = _

  //var bookId: java.lang.Long = _

  var index: java.lang.Integer = _

  var title: String = _

  var position: java.lang.Long = _
}
