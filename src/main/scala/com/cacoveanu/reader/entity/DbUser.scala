package com.cacoveanu.reader.entity

import javax.persistence.{Column, Entity, GeneratedValue, GenerationType, Id}

@Entity
class DbUser {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: java.lang.Long = _

  @Column(nullable = false, unique = true)
  var username: String = _

  var password: String = _
}