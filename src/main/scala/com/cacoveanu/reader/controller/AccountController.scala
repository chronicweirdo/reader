package com.cacoveanu.reader.controller

import com.cacoveanu.reader.entity.Book

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.cacoveanu.reader.service.{BookService, UserService}
import com.cacoveanu.reader.util.{DateUtil, SessionUtil}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.{RequestBody, RequestMapping, RequestMethod, RequestParam, ResponseBody}
import org.springframework.web.servlet.view.RedirectView

import java.text.SimpleDateFormat
import scala.jdk.CollectionConverters._
import scala.beans.BeanProperty

@Controller
class AccountController @Autowired()(private val accountService: UserService,
                                     private val bookService: BookService) {

  private val CSV_PARSING_REGEX = "(?!\\B[\"'][^\"']*),(?![^\"']*[\"']\\B)"

  @RequestMapping(value=Array("/password"), method = Array(RequestMethod.GET))
  def passwordResetPage(): String = "passwordReset"

  @RequestMapping(
    value=Array("/login"),
    method=Array(RequestMethod.GET)
  )
  def showLoginForm(): String = {
    val authentication: Authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.isInstanceOf[AnonymousAuthenticationToken]) {
      "login"
    } else {
      "redirect:/";
    }
  }

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

  @RequestMapping(
    value=Array("/exportProgress"),
    method=Array(RequestMethod.GET),
    produces=Array(MediaType.TEXT_PLAIN_VALUE)
  )
  @ResponseBody
  def exportProgress() = {
    val progress = bookService.loadAllProgress()
    val books: Map[String, Book] = progress.map(p => p.bookId).distinct.flatMap(id => bookService.loadBook(id)).map(b => (b.id, b)).toMap
    progress.map(p =>
      p.user.username + ","
      +wrapInQuotes(books.get(p.bookId).map(_.author).getOrElse("")) + ","
      +wrapInQuotes(p.title) + ","
      +wrapInQuotes(p.collection) + ","
      +p.position + ","
      +p.finished + ","
      +DateUtil.format(p.lastUpdate) + ","
      +p.bookId
    ).mkString("\n")
  }

  @RequestMapping(
    value=Array("/exportUsers"),
    method=Array(RequestMethod.GET),
    produces=Array(MediaType.TEXT_PLAIN_VALUE)
  )
  @ResponseBody
  def exportUsers() = {
    accountService.loadAllUsers().map(u => u.username + "," + u.password).mkString("\n")
  }

  @RequestMapping(
    value=Array("/import"),
    method=Array(RequestMethod.GET)
  )
  def importPage(@RequestParam(name = "message", required = false) message: String, model: Model) = {
    model.addAttribute("message", message)
    "import"
  }

  private def wrapInQuotes(s: String) = {
    if (s.contains(",")) {
      if (!s.contains("\"")) "\"" + s + "\""
      else if (!s.contains("'")) "'" + s + "'"
      else s
    } else s
  }

  private def trimQuotes(s: String) = {
    if (s.startsWith("\"") && s.endsWith("\"")) s.substring(1, s.length - 1)
    else if (s.startsWith("'") && s.endsWith("'")) s.substring(1, s.length - 1)
    else s
  }

  @RequestMapping(
    value=Array("/import"),
    method=Array(RequestMethod.POST),
    consumes=Array(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  )
  def importData(body: ImportForm, model: Model) = {
    val message = if (body.action == "progress") {
      val linesToImport = body.data.split("\r?\n")
      val unsavedProgress = linesToImport
        .flatMap(line => {
          val tokens = line.split(CSV_PARSING_REGEX).toSeq.map(trimQuotes)
          val result = if (tokens.size == 8) {
            bookService.importProgress(tokens(0), tokens(1), tokens(2), tokens(3), tokens(4), tokens(5), tokens(6), tokens(7))
          } else if (tokens.size == 7) {
            bookService.importProgress(tokens(0), tokens(1), tokens(2), tokens(3), tokens(4), tokens(5), tokens(6), null)
          } else if (tokens.size == 6) {
            bookService.importProgress(tokens(0), tokens(1), tokens(2), tokens(3), tokens(4), tokens(5), null, null)
          } else if (tokens.size == 5) {
            // section entry for progress is ignored
            bookService.importProgress(tokens(0), tokens(1), tokens(2), null, tokens(3), tokens(4), null, null)
          } else None
          result match {
            case Some(data) => None
            case None => Some(line)
          }
        })
      s"successfully added ${linesToImport.size - unsavedProgress.size} progress" + (
        if (unsavedProgress.size > 0) "<br>failed to import:<br><br>" + unsavedProgress.mkString("<br>")
        else ""
      )
    } else if (body.action == "addUsers") {
      val savedUsers = body.data
        .split("\r?\n")
        .map(line => {
          val tokens = line.split(",").toSeq
          if (tokens.size == 2) {
            accountService.addUser(tokens(0), tokens(1))
          } else {
            false
          }
        })
        .count(_ == true)
      s"successfully added $savedUsers users"
    } else if (body.action == "importUsers") {
      val savedUsers = body.data
        .split("\r?\n")
        .map(line => {
          val tokens = line.split(",").toSeq
          if (tokens.size == 2) {
            accountService.importUser(tokens(0), tokens(1))
          } else {
            false
          }
        })
        .count(_ == true)
      s"successfully added $savedUsers users"
    } else if (body.action == "deleteUsers") {
      val deletedUsers = body.data
        .split("\r?\n")
        .map(line => {
          accountService.deleteUser(line)
        })
        .count(_ == true)
      s"successfully deleted $deletedUsers users"
    } else {
      "nothing done"
    }
    //new RedirectView("/import?message=" + URLEncoder.encode(message, StandardCharsets.UTF_8.name()))
    model.addAttribute("message", message)
    "import"
  }

}

class ChangePasswordForm {
  @BeanProperty var oldPassword: String = _
  @BeanProperty var newPassword: String = _
  @BeanProperty var newPasswordConfirm: String = _
}

class ImportForm {
  @BeanProperty var data: String = _
  @BeanProperty var action: String = _
}