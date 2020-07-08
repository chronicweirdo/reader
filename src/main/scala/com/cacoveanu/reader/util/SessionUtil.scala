package com.cacoveanu.reader.util

import com.cacoveanu.reader.entity.Account
import com.cacoveanu.reader.service.UserPrincipal
import org.springframework.security.core.context.SecurityContextHolder

object SessionUtil {

  def getUser(): Account =
    SecurityContextHolder
      .getContext()
      .getAuthentication()
      .getPrincipal
      .asInstanceOf[UserPrincipal]
      .user

}
