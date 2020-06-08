package com.cacoveanu.reader.entity

import java.util.Date

import javax.persistence.{CascadeType, Entity, FetchType, GeneratedValue, GenerationType, Id, JoinColumn, ManyToOne, Table, UniqueConstraint}
import org.hibernate.annotations.{OnDelete, OnDeleteAction}

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
  @OnDelete(action = OnDeleteAction.CASCADE)
  var comic: DbComic = _

  var page: Int = _

  var totalPages: Int = _

  var lastUpdate: Date = _

  def this(user: DbUser, comic: DbComic, page: Int, totalPages: Int, lastUpdate: Date) = {
    this()
    this.user = user
    this.comic = comic
    this.page = page
    this.totalPages = totalPages
    this.lastUpdate = lastUpdate
  }
}
