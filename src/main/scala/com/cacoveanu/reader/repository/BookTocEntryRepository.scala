package com.cacoveanu.reader.repository

import com.cacoveanu.reader.entity.BookTocEntry
import org.springframework.data.jpa.repository.JpaRepository

trait BookTocEntryRepository extends JpaRepository[BookTocEntry, java.lang.Long] {
}
