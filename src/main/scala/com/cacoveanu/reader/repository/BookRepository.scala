package com.cacoveanu.reader.repository

import com.cacoveanu.reader.entity.{Book}
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.{JpaRepository, Query}
import org.springframework.data.repository.query.Param

trait BookRepository extends JpaRepository[Book, java.lang.Long] {

  @Query(
    value="select * from book b where lower(b.collection) + '\\' + lower(b.title) like :term order by lower(b.collection) + '\\' + lower(b.title) asc",
    countQuery = "select count(*) from book b where lower(b.collection) + '\\' + lower(b.title) like :term order by lower(b.collection) + '\\' + lower(b.title) asc",
    nativeQuery = true
  )
  def search(@Param("term") term: String, pageable: Pageable): java.util.List[Book]

  @Query(
    value="select distinct(collection) dc from book order by lower(dc) asc",
    nativeQuery = true
  )
  def findAllCollections(): java.util.List[String]

  def findByAuthorAndTitle(author: String, title: String): java.util.List[Book]

  @Query(
    value="select path from book",
    nativeQuery = true
  )
  def findAllPaths(): java.util.List[String]

  def findByIdNotIn(ids: java.util.List[java.lang.Long]): java.util.List[Book]

  def findByPathIn(paths: java.util.List[String]): java.util.List[Book]
}
