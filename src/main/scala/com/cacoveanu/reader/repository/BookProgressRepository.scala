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

  @Query(
    value="select * from book_progress p where p.user_id=:#{#user.id} and (p.finished is False)",
    countQuery = "select count(*) from book_progress p where p.user_id=:#{#user.id} and (p.finished is False)",
    nativeQuery = true
  )
  def findUnreadByUser(@Param("user") user: DbUser, pageable: Pageable): java.util.List[BookProgress]
}
