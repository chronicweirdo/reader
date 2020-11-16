package com.cacoveanu.reader.entity

import java.beans.Transient

import com.cacoveanu.reader.util.EpubUtil
import javax.persistence.{Entity, GeneratedValue, GenerationType, Id, Table, UniqueConstraint}

object TocEntry {
  val OPF = "opf"
  val NCX = "ncx" // this is from toc.ncx
  val CONTENT = "content"
}

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

  var source: String = _

  @Transient
  def resource: String = EpubUtil.baseLink(link)


  override def toString: String =
    s"$source\t$index\t$title\t$link"

  def this(source: String, index: Int, title: String, link: String) = {
    this()
    this.source = source
    this.index = index
    this.title = title
    this.link = link
  }
}