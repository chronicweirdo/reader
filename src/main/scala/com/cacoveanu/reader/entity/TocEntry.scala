package com.cacoveanu.reader.entity

import java.beans.Transient

import com.cacoveanu.reader.util.EpubUtil
import javax.persistence.{Entity, GeneratedValue, GenerationType, Id, Table, UniqueConstraint}

@Entity
class TocEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: java.lang.Long = _

  var index: Int = _

  var title: String = _

  var link: String = _

  var start: Int = _

  var size: Int = _

  @Transient
  def resource: String = EpubUtil.baseLink(link)

  def this(index: Int, title: String, link: String) = {
    this()
    this.index = index
    this.title = title
    this.link = link
  }
}