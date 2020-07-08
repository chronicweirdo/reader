package com.cacoveanu.reader.repository

import com.cacoveanu.reader.entity.TocEntry
import org.springframework.data.jpa.repository.JpaRepository

trait TocEntryRepository extends JpaRepository[TocEntry, java.lang.Long] {
}
