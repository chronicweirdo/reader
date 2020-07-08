package com.cacoveanu.reader.repository

import java.util.Optional

import com.cacoveanu.reader.entity.{Account, Setting}
import org.springframework.data.jpa.repository.JpaRepository

trait SettingRepository extends JpaRepository[Setting, java.lang.Long] {

  def findByUser(user: Account): java.util.List[Setting]

  def findByUserAndName(user: Account, name: String): Optional[Setting]

}
