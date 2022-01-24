package com.cacoveanu.reader.repository

import com.cacoveanu.reader.entity.Progress
import org.springframework.data.jpa.repository.{JpaRepository, Query}
import org.springframework.data.repository.query.Param

import java.util.Optional

trait ProgressRepository extends JpaRepository[Progress, java.lang.Long] {

  def findByUsername(username: String): java.util.List[Progress]

  def findByUsernameAndBookId(username: String, bookId: String): Optional[Progress]

  def findByUsernameAndBookIdIn(username: String, bookIds: java.util.List[String]): java.util.List[Progress]

  def findByBookIdIn(bookIds: java.util.List[String]): java.util.List[Progress]

  def findByBookId(bookId: String): java.util.List[Progress]

  def findByTitleAndCollection(title: String, collection: String): java.util.List[Progress]

  @Query(
    value="select * from progress p where p.username=:#{#username} and (p.finished is False)",
    countQuery = "select count(*) from progress p where p.username=:#{#username} and (p.finished is False)",
    nativeQuery = true
  )
  def findUnreadByUsername(@Param("username") username: String): java.util.List[Progress]
}
