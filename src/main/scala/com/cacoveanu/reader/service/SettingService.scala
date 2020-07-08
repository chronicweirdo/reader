package com.cacoveanu.reader.service

import com.cacoveanu.reader.entity.Setting
import com.cacoveanu.reader.repository.SettingRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

import scala.jdk.CollectionConverters._
import com.cacoveanu.reader.util.OptionalUtil.AugmentedOptional

import scala.beans.BeanProperty

@Service
class SettingService {

  //SecurityContextHolder.getContext().getAuthentication()
  @BeanProperty
  @Autowired
  var settingRepository: SettingRepository = _

  private val defaults = Map(
    Setting.BOOK_ZOOM -> "1.2"
  )

  private def getUser() = {
    SecurityContextHolder.getContext().getAuthentication().getPrincipal.asInstanceOf[UserPrincipal].user
  }

  private def getDefault(name: String): String = {
    defaults.getOrElse(name, "")
  }

  def getSetting(name: String): String = {
    settingRepository
      .findByUserAndName(getUser(), name)
      .asScala
      .map(_.value)
      .getOrElse(getDefault(name))
  }

  def saveSetting(name: String, value: String) = {
    val user = getUser()
    val newSetting = settingRepository.findByUserAndName(user, name).asScala match {
      case Some(setting) =>
        setting.value = value
        setting
      case None =>
        new Setting(user, name, value)
    }
    settingRepository.save(newSetting)
  }
}
