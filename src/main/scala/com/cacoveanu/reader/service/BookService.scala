package com.cacoveanu.reader.service

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Date

import com.cacoveanu.reader.entity.{Account, Book, CollectionNode, Progress}
import com.cacoveanu.reader.repository.{AccountRepository, BookRepository, ProgressRepository}
import com.cacoveanu.reader.util.OptionalUtil.AugmentedOptional
import com.cacoveanu.reader.util.SessionUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.{PageRequest, Sort}
import org.springframework.stereotype.Service

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._
import scala.util.matching.Regex

@Service
class BookService {

  private val PAGE_SIZE = 20

  @BeanProperty
  @Autowired
  var bookRepository: BookRepository = _

  @BeanProperty
  @Autowired
  var accountRepository: AccountRepository = _

  @BeanProperty
  @Autowired
  var progressRepository: ProgressRepository = _

  def loadBook(id: java.lang.Long) = {
    bookRepository.findById(id).asScala
  }

  def loadProgress(account: Account, id: java.lang.Long) = {
    progressRepository.findByUserAndBookId(account, id).asScala
  }

  def loadProgress(book: Book): Option[Progress] = {
    progressRepository.findByUserAndBook(SessionUtil.getUser(), book).asScala
  }

  def loadProgress(books: Seq[Book]): Seq[Progress] = {
    progressRepository.findByUserAndBookIn(SessionUtil.getUser(), books.asJava).asScala.toSeq
  }

  def loadAllProgress() = {
    progressRepository.findAll().asScala.toSeq
  }

  def importProgress(username: String, author: String, title: String, section: String, position: String, finished: String) = {
    try {
      val user = Option(accountRepository.findByUsername(username))
      val matchingBooks = bookRepository.findByAuthorAndTitle(author, title).asScala
      val matchingBook = if (matchingBooks.size == 1) matchingBooks.headOption else None
      val positionParsed = position.toIntOption
      val finishedParsed = finished.toBooleanOption
      (user, matchingBook, positionParsed, finishedParsed) match {
        case (Some(u), Some(b), Some(p), Some(f)) =>
          Option(progressRepository.save(new Progress(u, b, p, new Date(), f)))

        case _ =>
          None
      }
    } catch {
      case _: Throwable => None
    }
  }

  def loadTopProgress(limit: Int): Seq[Progress] = {
    val sort = Sort.by(Direction.DESC, "last_update")
    val pageRequest = PageRequest.of(0, limit, sort)
    val user = SessionUtil.getUser()
    progressRepository.findUnreadByUser(user, pageRequest).asScala.toSeq
  }

  def saveProgress(bookId: java.lang.Long, position: Int) = {
    loadBook(bookId) match {
      case Some(book) =>
        val finished = position >= book.size - 1
        val progress = new Progress(SessionUtil.getUser(), book, position, new Date(), finished)
        progressRepository.findByUserAndBookId(progress.user, bookId).asScala.foreach(p => progress.id = p.id)
        progressRepository.save(progress)
        true

      case _ =>
        false
    }
  }

  def deleteProgress(bookId: java.lang.Long) = {
    progressRepository
      .findByUserAndBookId(SessionUtil.getUser(), bookId).asScala
      .map(progress => {
        progressRepository.delete(progress)
        true
      })
      .getOrElse(false)
  }

  /* collections code */

  def loadCollections(): Seq[String] = {
    bookRepository.findAllCollections().asScala.toSeq
  }

  def loadCollectionsTree(): CollectionNode = {
    val collections = bookRepository.findAllCollections().asScala.toSeq
    val root = new CollectionNode(CollectionNode.EVERYTHING, "")
    collections.filter(_.nonEmpty).foreach(collection => {
      val entries = collection.split("\\\\")
      var current = root
      var currentSearch = ""
      for (i <- entries.indices) {
        val col = entries(i)
        currentSearch += col
        current.children.find(n => n.name == col) match {
          case Some(child) =>
            current = child
          case None =>
            val newChild = new CollectionNode(col, currentSearch)
            current.children = current.children :+ newChild
            current = newChild
        }
        currentSearch += "\\"
      }
    })
    root
  }

  private def prepareSearchTerm(original: String): String = {
    val lowercase = original.toLowerCase()
    val pattern = "[A-Za-z0-9]+".r
    val matches: Regex.MatchIterator = pattern.findAllIn(lowercase)
    val result = "%" + matches.mkString("%") + "%"
    result
  }

  def search(term: String, page: Int): Seq[Book] = {
    val sort = Sort.by(Direction.ASC, "collection", "title")
    val pageRequest = PageRequest.of(page, PAGE_SIZE, sort)
    bookRepository.search(prepareSearchTerm(term), pageRequest).asScala.toSeq
  }

  def getCollectionPage(page: Int): Seq[Book] = {
    val sort = Sort.by(Direction.ASC, "collection", "title")
    val pageRequest = PageRequest.of(page, PAGE_SIZE, sort)
    bookRepository.findAll(pageRequest).asScala.toSeq
  }

  def loadBooks = {
    bookRepository.findAll().asScala.toSeq
      .filter(b => b.path.endsWith(".epub"))
  }
}

