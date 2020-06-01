package com.cacoveanu.reader.repository

import com.cacoveanu.reader.entity.{ComicProgress, DbComic, DbUser}
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.{JpaRepository, Query}

trait ComicProgressRepository extends JpaRepository[ComicProgress, java.lang.Long] {

  def findByUserAndComic(user: DbUser, comic: DbComic): ComicProgress

  def findByUser(user: DbUser): java.util.List[ComicProgress]

  @Query(
    value="select * from comic_progress c where (c.page < c.total_pages - 1)",
    countQuery = "select * from comic_progress c where (c.page < c.total_pages - 1)",
    nativeQuery = true
  )
  def findUnreadByUser(user: DbUser, pageable: Pageable): java.util.List[ComicProgress]

  def findByUserAndComicIn(user: DbUser, comics: java.util.List[DbComic]): java.util.List[ComicProgress]
}
