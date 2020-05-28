package com.cacoveanu.reader.repository


import com.cacoveanu.reader.entity.DbComic
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

trait ComicRepository extends JpaRepository[DbComic, java.lang.Long] {

  def findAllByOrderByCollectionAsc(pageable: Pageable): java.util.List[DbComic]

}
