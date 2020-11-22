package com.cacoveanu.reader.repository

import com.cacoveanu.reader.entity.BookLink
import org.springframework.data.jpa.repository.JpaRepository

trait BookLinkRepository extends JpaRepository[BookLink, java.lang.Long] {
}
