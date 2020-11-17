package com.cacoveanu.reader.repository

import com.cacoveanu.reader.entity.BookResource
import org.springframework.data.jpa.repository.JpaRepository

trait BookResourceRepository extends JpaRepository[BookResource, java.lang.Long] {
}
