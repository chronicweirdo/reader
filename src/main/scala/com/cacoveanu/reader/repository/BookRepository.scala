package com.cacoveanu.reader.repository

import com.cacoveanu.reader.entity.{DbBook}
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.{JpaRepository, Query}
import org.springframework.data.repository.query.Param

trait BookRepository extends JpaRepository[DbBook, String] {

  @Query(
    value="select * from db_book b where lower(b.title) like %:term% or lower(b.collection) like %:term% order by b.collection asc",
    countQuery = "select count(*) from db_book b where lower(b.title) like %:term% or lower(b.collection) like %:term% order by b.collection asc",
    nativeQuery = true
  )
  def search(@Param("term") term: String, pageable: Pageable): java.util.List[DbBook]

  @Query(
    value="select distinct(collection) dc from db_book order by dc asc",
    nativeQuery = true
  )
  def findAllCollections(): java.util.List[String]
}
