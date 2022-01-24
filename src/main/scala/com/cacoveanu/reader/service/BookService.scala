package com.cacoveanu.reader.service

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Date
import com.cacoveanu.reader.entity.{Account, Book, CollectionNode, Progress}
import com.cacoveanu.reader.repository.{AccountRepository, BookRepository, ProgressRepository}
import com.cacoveanu.reader.util.OptionalUtil.AugmentedOptional
import com.cacoveanu.reader.util.{DateUtil, SessionUtil}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.{PageRequest, Sort}
import org.springframework.stereotype.Service

import java.lang
import java.text.SimpleDateFormat
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

  def loadBook(id: String) = {
    bookRepository.findById(id).asScala
  }

  def loadProgress(account: Account, id: String) = {
    progressRepository.findByUserAndBookId(account, id).asScala
  }

  def loadProgress(bookId: String): Option[Progress] = {
    progressRepository.findByUserAndBookId(SessionUtil.getUser(), bookId).asScala
  }

  def loadProgress(books: Seq[Book]): Seq[Progress] = {
    progressRepository.findByUserAndBookIdIn(SessionUtil.getUser(), books.map(b => b.id).asJava).asScala.toSeq
  }

  def loadAllProgress() = {
    progressRepository.findAll().asScala.toSeq
  }

  def importProgressLegacy(username: String, author: String, title: String, collection: String, position: String, finished: String, lastUpdate: String) = {
    // todo: have legacy and new progress import, legacy looks at title, collection, but new one doesn't even try to find book, just imports the progress on the checksum
    // todo: take checksum into account when importing progress
    try {
      val user = Option(accountRepository.findByUsername(username))
      var matchingBook: Option[Book] = None
      val matchingBooks = bookRepository.findByTitle(title).asScala
      matchingBook = if (matchingBooks.size == 1) {
        matchingBooks.headOption
      } else if (matchingBooks.size > 1) {
        // try to pick the one with the correct collection
        matchingBooks.find(b => b.collection == collection)
      } else None

      val positionParsed = position.toIntOption
      val finishedParsed = finished.toBooleanOption
      val lastUpdateDate = DateUtil.parse(lastUpdate)
      // try to find existing progress
      val existingProgressId: Option[lang.Long] = (user, matchingBook) match {
        case (Some(u), Some(b)) => progressRepository.findByUserAndBookId(u, b.id).asScala.map(_.id)
        case _ => None
      }

      (user, matchingBook, positionParsed, finishedParsed, lastUpdateDate) match {
        case (Some(u), Some(b), Some(p), Some(f), Some(d)) =>
          val progress = new Progress(u, b, p, d, f)
          if (existingProgressId.isDefined) progress.id = existingProgressId.get
          Option(progressRepository.save(progress))
        case (Some(u), Some(b), Some(p), Some(f), None) =>
          val progress = new Progress(u, b, p, new Date(), f)
          if (existingProgressId.isDefined) progress.id = existingProgressId.get
          Option(progressRepository.save(progress))
        case _ =>
          None
      }
    } catch {
      case _: Throwable => None
    }
  }

  def loadValidTopProgress(limit: Int): Seq[Progress] = {
    val user = SessionUtil.getUser()
    val userProgress = progressRepository.findUnreadByUser(user).asScala.toSeq
    userProgress.sortBy(p => p.lastUpdate).reverse.filter(p => loadBook(p.bookId).isDefined).take(limit)
  }

  def loadValidReadInformation(): Seq[Progress] = {
    val user = SessionUtil.getUser()
    progressRepository.findByUser(user).asScala.toSeq.sortBy(p => p.lastUpdate).reverse.filter(p => loadBook(p.bookId).isDefined)
  }

  def loadAllReadInformation(): Seq[(Progress, Option[Book])] = {
    val user = SessionUtil.getUser()
    progressRepository.findByUser(user).asScala.toSeq.sortBy(p => p.lastUpdate).reverse.map(p => (p, loadBook(p.bookId)))
  }

  def saveProgress(bookId: String, position: Int) = {
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

  def deleteProgress(bookId: String) = {
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
      val entries = collection.split("/").filter(_.nonEmpty)
      var current = root
      var currentSearch = "/"
      for (i <- entries.indices) {
        val col = entries(i)
        currentSearch += col
        current.children.asScala.find(n => n.name == col) match {
          case Some(child) =>
            current = child
          case None =>
            val newChild = new CollectionNode(col, currentSearch)
            current.children.add(newChild)
            current = newChild
        }
        currentSearch += "/"
      }
    })
    root
  }

  private def prepareSearchTerm(original: String): String = {
    if (original.startsWith("/")) {
      // very specific search that must start with this
      original.toLowerCase() + "%"
    } else if (original.contains("/")) {
      // very specific search that must contain this path - allows searches for subpaths if search term starts with a space
      "%" + original.toLowerCase().trim + "%"
    } else {
      // this is a more general search for terms
      val lowercase = original.toLowerCase()
      val pattern = "[A-Za-z0-9]+".r
      val matches: Regex.MatchIterator = pattern.findAllIn(lowercase)
      val result = "%" + matches.mkString("%") + "%"
      result
    }
  }

  def loadLatestAdded(limit: Int): Seq[Book] = {
    val pageRequest = PageRequest.of(0, limit)
    bookRepository.findLatestAdded(pageRequest).asScala.toSeq
  }

  def search(term: String, page: Int): Seq[Book] = {
    val pageRequest = PageRequest.of(page, PAGE_SIZE)
    bookRepository.search(prepareSearchTerm(term), pageRequest).asScala.toSeq
  }

  def getCollectionPage(page: Int): Seq[Book] = {
    val pageRequest = PageRequest.of(page, PAGE_SIZE)
    bookRepository.search("%", pageRequest).asScala.toSeq
  }

  def loadBooks = {
    bookRepository.findAll().asScala.toSeq
      .filter(b => b.path.endsWith(".epub"))
  }
}

