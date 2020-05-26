package com.cacoveanu.reader.entity

import javax.persistence.{Entity, FetchType, GeneratedValue, GenerationType, Id, JoinColumn, ManyToOne, Table, UniqueConstraint}

@Entity
@Table(uniqueConstraints=Array(new UniqueConstraint(columnNames = Array("userId", "comicId"))))
class ComicProgress {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: java.lang.Long = _

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name="userId")
  var user: DbUser = _

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name="comicId")
  var comic: DbComic = _

  var page: Int = _

  var totalPages: Int = _

  def this(user: DbUser, comic: DbComic, page: Int, totalPages: Int) {
    this()
    this.user = user
    this.comic = comic
    this.page = page
    this.totalPages = totalPages
  }
}
