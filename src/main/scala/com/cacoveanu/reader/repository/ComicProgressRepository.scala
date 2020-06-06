package com.cacoveanu.reader.repository

import java.util.Optional

import com.cacoveanu.reader.entity.{ComicProgress, DbComic, DbUser}
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.{JpaRepository, Query}
import org.springframework.data.repository.query.Param

trait ComicProgressRepository extends JpaRepository[ComicProgress, java.lang.Long] {

  def findByUserAndComic(user: DbUser, comic: DbComic): ComicProgress

  def findByUser(user: DbUser): java.util.List[ComicProgress]

  def findByUserAndComicId(user: DbUser, comicId: Long): Optional[ComicProgress]

  @Query(
    value="select * from comic_progress c where c.user_id=:#{#user.id} and (c.page < c.total_pages - 1)",
    countQuery = "select * from comic_progress c where c.user_id=:#{#user.id} and (c.page < c.total_pages - 1)",
    nativeQuery = true
  )
  def findUnreadByUser(@Param("user") user: DbUser, pageable: Pageable): java.util.List[ComicProgress]

  def findByUserAndComicIn(user: DbUser, comics: java.util.List[DbComic]): java.util.List[ComicProgress]
}
