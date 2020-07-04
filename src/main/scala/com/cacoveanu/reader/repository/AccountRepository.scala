package com.cacoveanu.reader.repository

import com.cacoveanu.reader.entity.Account
import org.springframework.data.jpa.repository.JpaRepository

trait AccountRepository extends JpaRepository[Account, java.lang.Long] {
  def findByUsername(username: String): Account
}
