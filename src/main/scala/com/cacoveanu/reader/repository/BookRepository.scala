package com.cacoveanu.reader.repository

import com.cacoveanu.reader.entity.DbBook
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.{JpaRepository, Query}
import org.springframework.data.repository.query.Param

trait BookRepository extends JpaRepository[DbBook, String] {
}
