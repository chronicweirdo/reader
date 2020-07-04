package com.cacoveanu.reader.repository

import java.util.Optional

import com.cacoveanu.reader.entity.{BookProgress, DbBook, DbUser}
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.{JpaRepository, Query}
import org.springframework.data.repository.query.Param

trait BookProgressRepository extends JpaRepository[BookProgress, java.lang.Long] {

  def findByUserAndBook(user: DbUser, book: DbBook): BookProgress

  def findByUser(user: DbUser): java.util.List[BookProgress]

  def findByUserAndBookId(user: DbUser, bookId: String): Optional[BookProgress]

  def findByUserAndBookIn(user: DbUser, books: java.util.List[DbBook]): java.util.List[BookProgress]
}
