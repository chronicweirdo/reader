package com.cacoveanu.reader.entity

import java.util.Date

import com.cacoveanu.reader.util.EpubUtil
import javax.persistence._
import org.hibernate.annotations.{OnDelete, OnDeleteAction}

import scala.jdk.CollectionConverters._

@Entity
@Table(uniqueConstraints=Array(new UniqueConstraint(columnNames = Array("userId", "bookId"))))
class Progress {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: java.lang.Long = _

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name="userId")
  var user: Account = _

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name="bookId")
  @OnDelete(action = OnDeleteAction.CASCADE)
  var book: Book = _

  var section: String = _

  var position: Int = _

  var lastUpdate: Date = _

  var finished: Boolean = _

  @Transient
  def getPositionInBook() = {
    if (section != null && section.length > 0) {
      val start = book.toc.asScala.find(e => EpubUtil.baseLink(e.link) == section).map(e => e.start).getOrElse(0)

      start + position
    } else {
      position
    }
  }

  def this(user: Account, book: Book, section: String, position: Int, lastUpdate: Date, finished: Boolean) = {
    this()
    this.user = user
    this.book = book
    this.section = section
    this.position = position
    this.lastUpdate = lastUpdate
    this.finished = finished
  }
}
