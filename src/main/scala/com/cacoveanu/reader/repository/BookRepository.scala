package com.cacoveanu.reader.repository

import com.cacoveanu.reader.entity.Book
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.{JpaRepository, Query}
import org.springframework.data.repository.query.Param

import java.util.Optional

trait BookRepository extends JpaRepository[Book, String] {

  @Query(
    value="select * from book b where lower(b.collection) + '/' + lower(b.title) like :term order by lower(b.collection) asc, lower(b.title) asc",
    countQuery = "select count(*) from book b where lower(b.collection) + '/' + lower(b.title) like :term order by lower(b.collection) asc, lower(b.title) asc",
    nativeQuery = true
  )
  def search(@Param("term") term: String, pageable: Pageable): java.util.List[Book]

  @Query(
    value="select * from book b where b.added is not null order by b.added desc",
    countQuery = "select count(*) from book b where b.added is not null order by b.added desc",
    nativeQuery = true
  )
  def findLatestAdded(pageable: Pageable): java.util.List[Book]

  @Query(
    value="select distinct(collection) dc from book order by lower(dc) asc",
    nativeQuery = true
  )
  def findAllCollections(): java.util.List[String]

  def findByTitle(title: String): java.util.List[Book]

  @Query(
    value="select (collection + '/' + title + '.' + file_type) from book",
    nativeQuery = true
  )
  def findAllPaths(): java.util.List[String]

  def findByIdNotIn(ids: java.util.List[String]): java.util.List[Book]

  def findByCollectionAndTitle(collection: String, title: String): Optional[Book]
}
