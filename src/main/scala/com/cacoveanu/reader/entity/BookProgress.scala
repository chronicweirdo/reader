package com.cacoveanu.reader.entity

import java.util.Date

import javax.persistence._
import org.hibernate.annotations.{OnDelete, OnDeleteAction}

@Entity
@Table(uniqueConstraints=Array(new UniqueConstraint(columnNames = Array("userId", "bookId"))))
class BookProgress {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: java.lang.Long = _

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name="userId")
  var user: DbUser = _

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name="bookId")
  @OnDelete(action = OnDeleteAction.CASCADE)
  var book: DbBook = _

  var section: String = _

  var position: Int = _

  var lastUpdate: Date = _

  var finished: Boolean = _

  def this(user: DbUser, book: DbBook, section: String, position: Int, lastUpdate: Date, finished: Boolean) = {
    this()
    this.user = user
    this.book = book
    this.section = section
    this.position = position
    this.lastUpdate = lastUpdate
    this.finished = finished
  }
}
