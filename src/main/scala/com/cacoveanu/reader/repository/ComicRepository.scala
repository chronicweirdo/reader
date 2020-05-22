package com.cacoveanu.reader.repository

import com.cacoveanu.reader.service.DbComic
import org.springframework.data.jpa.repository.JpaRepository

trait ComicRepository extends JpaRepository[DbComic, String] {

}
