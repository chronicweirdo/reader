package com.cacoveanu.reader.repository

import java.util.Optional

import com.cacoveanu.reader.entity.{Progress, Book, Account}
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.{JpaRepository, Query}
import org.springframework.data.repository.query.Param

trait ProgressRepository extends JpaRepository[Progress, java.lang.Long] {

  def findByUser(user: Account): java.util.List[Progress]

  def findByUserAndBookId(user: Account, bookId: String): Optional[Progress]

  def findByUserAndBookIdIn(user: Account, bookIds: java.util.List[String]): java.util.List[Progress]

  def findByBookIdIn(bookIds: java.util.List[String]): java.util.List[Progress]

  @Query(
    value="select * from progress p where p.user_id=:#{#user.id} and (p.finished is False)",
    countQuery = "select count(*) from progress p where p.user_id=:#{#user.id} and (p.finished is False)",
    nativeQuery = true
  )
  def findUnreadByUser(@Param("user") user: Account): java.util.List[Progress]
}
