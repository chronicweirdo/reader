package com.cacoveanu.reader.repository

import com.cacoveanu.reader.service.{ComicProgress, DbComic, DbUser}
import org.springframework.data.jpa.repository.JpaRepository

trait ComicProgressRepository extends JpaRepository[ComicProgress, java.lang.Long] {

  def findByUserAndComic(user: DbUser, comic: DbComic): ComicProgress
}
