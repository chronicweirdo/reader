package com.cacoveanu.reader.controller

import com.cacoveanu.reader.service.UserService
import com.cacoveanu.reader.util.SessionUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}
import org.springframework.web.servlet.view.RedirectView

import scala.jdk.CollectionConverters._
import scala.beans.BeanProperty

@Controller
class AccountController @Autowired()(private val accountService: UserService) {

  @RequestMapping(value=Array("/password"), method = Array(RequestMethod.GET))
  def passwordResetPage(): String = "passwordReset"

  @RequestMapping(
    value=Array("/password"),
    method=Array(RequestMethod.POST),
    consumes=Array(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  )
  @ResponseBody
  def passwordResetAction(body: ChangePasswordForm): RedirectView = {
    if (body.newPassword != body.newPasswordConfirm) {
      new RedirectView("/password?error")
    } else {
      if (accountService.changePassword(SessionUtil.getUser(), body.oldPassword, body.newPassword))
        new RedirectView("/")
      else
        new RedirectView("/password?error")
    }
  }

  @RequestMapping(value=Array("/users"), method = Array(RequestMethod.GET))
  def usersManagementPage(model: Model): String = {
    model.addAttribute("users", accountService.loadAllUsers.map(_.username).asJava)
    "users"
  }

  @RequestMapping(
    value=Array("/deleteUser"),
    method=Array(RequestMethod.GET)
  )
  def deleteUser(@RequestParam("name") name: String) = {
    if (accountService.deleteUser(name)) {
      new RedirectView("/users")
    } else {
      new RedirectView("/users?deleteError")
    }
  }

  @RequestMapping(
    value=Array("/addUser"),
    method=Array(RequestMethod.POST),
    consumes=Array(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  )
  def addUser(body: AddUserForm): RedirectView = {
    if (accountService.addUser(body.username, body.password)) {
      new RedirectView("/users")
    } else {
      new RedirectView("/users?addError")
    }
  }
}

class ChangePasswordForm {
  @BeanProperty var oldPassword: String = _
  @BeanProperty var newPassword: String = _
  @BeanProperty var newPasswordConfirm: String = _
}

class AddUserForm {
  @BeanProperty var username: String = _
  @BeanProperty var password: String = _
}