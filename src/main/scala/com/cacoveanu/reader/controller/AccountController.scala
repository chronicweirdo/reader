package com.cacoveanu.reader.controller

import com.cacoveanu.reader.service.{BookService, UserService}
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
class AccountController @Autowired()(private val accountService: UserService,
                                     private val bookService: BookService) {

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
    value=Array("/exportProgress"),
    method=Array(RequestMethod.GET),
    produces=Array(MediaType.TEXT_PLAIN_VALUE)
  )
  @ResponseBody
  def exportProgress() = {
    bookService.loadAllProgress().map(p =>
      p.user.username + ","
        + p.book.author + ","
        + p.book.title + ","
        + p.section + ","
        + p.position + ","
        + p.finished
    ).mkString("\n")
  }

  @RequestMapping(
    value=Array("/importProgress"),
    method=Array(RequestMethod.GET)
  )
  def importProgressPage() = "importProgress"

  @RequestMapping(
    value=Array("/importProgress"),
    method=Array(RequestMethod.POST),
    consumes=Array(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  )
  @ResponseBody
  def importProgress(body: ImportProgressForm) = {
    val savedProgress = body.progress.split("\r?\n")
      .flatMap(line => {
        val tokens = line.split(",").toSeq
        if (tokens.size == 6) {
          bookService.importProgress(tokens(0), tokens(1), tokens(2), tokens(3), tokens(4), tokens(5))
        } else None
      })
    s"successfully added ${savedProgress.size} progress"
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

class ImportProgressForm {
  @BeanProperty var progress: String = _
}