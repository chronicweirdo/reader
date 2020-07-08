package com.cacoveanu.reader.entity

import javax.persistence._

object Setting {
  val BOOK_ZOOM = "bookZoom"
}

@Entity
@Table(uniqueConstraints=Array(new UniqueConstraint(columnNames = Array("userId", "name"))))
class Setting {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: java.lang.Long = _

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name="userId")
  var user: Account = _

  var name: String = _

  var value: String = _

  def this(user: Account, name: String, value: String) = {
    this()
    this.user = user
    this.name = name
    this.value = value
  }
}
