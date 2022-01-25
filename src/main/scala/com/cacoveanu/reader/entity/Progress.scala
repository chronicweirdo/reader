package com.cacoveanu.reader.entity

import java.util.Date
import javax.persistence._

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

  def this(username: String, bookId: String, title: String, collection: String, position: Int, size: Int, lastUpdate: Date, finished: Boolean) = {
    this()
    this.username = username
    this.bookId = bookId
    this.title = title
    this.collection = collection
    this.position = position
    this.size = size
    this.lastUpdate = lastUpdate
    this.finished = finished
  }
}
