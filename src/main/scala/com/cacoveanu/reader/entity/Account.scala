package com.cacoveanu.reader.entity

import javax.persistence.{Column, Entity, GeneratedValue, GenerationType, Id}

import scala.beans.BeanProperty

@Entity
class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @BeanProperty
  var id: java.lang.Long = _

  @Column(nullable = false, unique = true)
  var username: String = _

  var password: String = _
}