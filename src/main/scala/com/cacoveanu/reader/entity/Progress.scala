package com.cacoveanu.reader.entity

import java.util.Date

import com.cacoveanu.reader.util.EpubUtil
import javax.persistence._
import org.hibernate.annotations.{OnDelete, OnDeleteAction}

import scala.jdk.CollectionConverters._

@Entity
@Table(uniqueConstraints=Array(new UniqueConstraint(columnNames = Array("username", "bookId"))))
class Progress {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: java.lang.Long = _

  var username: String = _

  var bookId: String = _

  var position: Int = _

  var lastUpdate: Date = _

  var finished: Boolean = _

  var title: String = _

  var collection: String = _

  var size: Int = _

  def this(username: String, book: Book, position: Int, lastUpdate: Date, finished: Boolean) = {
    this()
    this.username = username
    this.bookId = book.id
    this.title = book.title
    this.size = book.size
    this.collection = book.collection
    this.position = position
    this.lastUpdate = lastUpdate
    this.finished = finished
  }
}
