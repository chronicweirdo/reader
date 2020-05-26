package com.cacoveanu.reader.repository

import com.cacoveanu.reader.entity.DbUser
import org.springframework.data.jpa.repository.JpaRepository

trait UserRepository extends JpaRepository[DbUser, java.lang.Long] {
  def findByUsername(username: String): DbUser
}
