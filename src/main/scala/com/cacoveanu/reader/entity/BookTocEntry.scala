package com.cacoveanu.reader.entity

import javax.persistence.{Column, Entity, GeneratedValue, GenerationType, Id}

@Entity
class BookTocEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: java.lang.Long = _

  var index: java.lang.Integer = _

  @Column(length = 3000)
  var title: String = _

  var position: java.lang.Long = _

  var level: java.lang.Integer = _
}
