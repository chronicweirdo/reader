package com.cacoveanu.reader.entity

import javax.persistence.{Entity, GeneratedValue, GenerationType, Id}

@Entity
class BookResource {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: java.lang.Long = _

  var path: String = _

  var start: java.lang.Long = _

  var end: java.lang.Long = _
}
