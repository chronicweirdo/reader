package com.cacoveanu.reader.repository


import com.cacoveanu.reader.entity.DbComic
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.{JpaRepository, Query}
import org.springframework.data.repository.query.Param

trait ComicRepository extends JpaRepository[DbComic, String] {

  //def findAll(pageable: Pageable): java.util.List[DbComic]

  @Query(
    value="select * from db_comic c where lower(c.title) like %:term% or lower(c.collection) like %:term% order by c.collection asc",
    countQuery = "select count(*) from db_comic c where lower(c.title) like %:term% or lower(c.collection) like %:term% order by c.collection asc",
    nativeQuery = true
  )
  def search(@Param("term") term: String, pageable: Pageable): java.util.List[DbComic]

  @Query(
    value="select distinct(collection) dc from db_comic order by dc asc",
    nativeQuery = true
  )
  def findAllCollections(): java.util.List[String]

  def findByPathIn(pathList: java.util.List[String]): java.util.List[DbComic]
}
