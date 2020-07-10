package com.cacoveanu.reader.repository

import com.cacoveanu.reader.entity.Section
import org.springframework.data.jpa.repository.JpaRepository

trait SectionRepository extends JpaRepository[Section, java.lang.Long] {
}
