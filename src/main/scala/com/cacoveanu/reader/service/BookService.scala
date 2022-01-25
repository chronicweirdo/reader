package com.cacoveanu.reader.service

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Date
import com.cacoveanu.reader.entity.{Account, Book, CollectionNode, Progress}
import com.cacoveanu.reader.repository.{AccountRepository, BookRepository, ProgressRepository}
import com.cacoveanu.reader.util.OptionalUtil.AugmentedOptional
import com.cacoveanu.reader.util.{DateUtil, ProgressUtil, SessionUtil}
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
    progressRepository.findByUsernameAndBookId(account.username, id).asScala
  }

  def loadProgress(bookId: String): Option[Progress] = {
    progressRepository.findByUsernameAndBookId(SessionUtil.getUser().username, bookId).asScala
  }

  def loadProgress(books: Seq[Book]): Seq[Progress] = {
    progressRepository.findByUsernameAndBookIdIn(SessionUtil.getUser().username, books.map(b => b.id).asJava).asScala.toSeq
  }

  def loadAllProgress() = {
    progressRepository.findAll().asScala.toSeq
  }

  def importProgress(username: String, bookId: String, title: String, collection: String, position: String, size: String, finished: String, lastUpdate: String) = {
    val positionInt = position.toIntOption
    val sizeInt = position.toIntOption
    val finishedBoolean = finished.toBooleanOption
    val lastUpdateDate = DateUtil.parse(lastUpdate)
    val existingProgressId = findMatchingProgressId(username, bookId)

    (positionInt, sizeInt, finishedBoolean, lastUpdateDate) match {
      case (Some(positionValue), Some(sizeValue), Some(finishedValue), Some(lastUpdateValue)) =>
        val progress = new Progress(username, bookId, title, collection, positionValue, sizeValue, lastUpdateValue, finishedValue)
        if (existingProgressId.isDefined) progress.id = existingProgressId.get
        val savedProgress: Option[Progress] = Option(progressRepository.save(progress))

        // verify if there is a book for this progress
        if (bookRepository.findById(bookId).isEmpty) {
          // try to match to some book
          findMatchingBook(title, collection) match {
            case Some(book) =>
              // create another progress for this matching book
              val secondProgress = ProgressUtil.fixProgressForBook(progress, book)
              findMatchingProgressId(username, book.id) match {
                case Some(id) => progress.id = id
              }
              progressRepository.save(secondProgress)
          }
        }

        savedProgress
      case _ => None
    }
  }

  def findMatchingBook(title: String, collection: String) = {
    val matchingBooks = bookRepository.findByTitle(title).asScala
    if (matchingBooks.size == 1) {
      matchingBooks.headOption
    } else if (matchingBooks.size > 1) {
      // try to pick the one with the correct collection
      matchingBooks.find(b => b.collection == collection)
    } else None
  }

  def findMatchingProgressId(username: String, bookId: String) = progressRepository.findByUsernameAndBookId(username, bookId).asScala.map(_.id)

  def importProgressLegacy(username: String, author: String, title: String, collection: String, positionString: String, finishedString: String, lastUpdateString: String) = {
    try {
      val matchingBook = findMatchingBook(title, collection)
      val positionParsed = positionString.toIntOption
      val finishedParsed = finishedString.toBooleanOption
      val lastUpdateDate = DateUtil.parse(lastUpdateString)

      // try to find existing progress
      val existingProgressId: Option[lang.Long] = matchingBook match {
        case Some(b) => findMatchingProgressId(username, b.id)
        case _ => None
      }

      // create or update progress
      val progressToSave = (matchingBook, positionParsed, finishedParsed, lastUpdateDate, existingProgressId) match {
        case (Some(book), Some(position), Some(finished), Some(lastUpdate), Some(progressId)) =>
          val progress = new Progress(username, book, position, lastUpdate, finished)
          progress.id = progressId
          Some(progress)
        case (Some(book), Some(position), Some(finished), Some(lastUpdate), None) =>
          Some(new Progress(username, book, position, lastUpdate, finished))
        case (Some(book), Some(position), Some(finished), None, Some(progressId)) =>
          val progress = new Progress(username, book, position, new Date(), finished)
          progress.id = progressId
          Some(progress)
        case (Some(book), Some(position), Some(finished), None, None) =>
          Some(new Progress(username, book, position, new Date(), finished))
        case _ =>
          None
      }

      progressToSave match {
        case Some(progress) => Option(progressRepository.save(progress))
        case None => None
      }
    } catch {
      case _: Throwable => None
    }
  }

  def loadValidTopProgress(limit: Int): Seq[Progress] = {
    val user = SessionUtil.getUser()
    val userProgress = progressRepository.findUnreadByUsername(user.username).asScala.toSeq
    userProgress.sortBy(p => p.lastUpdate).reverse.filter(p => loadBook(p.bookId).isDefined).take(limit)
  }

  def loadValidReadInformation(): Seq[Progress] = {
    val user = SessionUtil.getUser()
    progressRepository.findByUsername(user.username).asScala.toSeq.sortBy(p => p.lastUpdate).reverse.filter(p => loadBook(p.bookId).isDefined)
  }

  def loadAllReadInformation(): Seq[(Progress, Option[Book])] = {
    val user = SessionUtil.getUser()
    progressRepository.findByUsername(user.username).asScala.toSeq.sortBy(p => p.lastUpdate).reverse.map(p => (p, loadBook(p.bookId)))
  }

  def saveProgress(bookId: String, position: Int) = {
    loadBook(bookId) match {
      case Some(book) =>
        val finished = position >= book.size - 1
        val progress = new Progress(SessionUtil.getUser().username, book, position, new Date(), finished)
        progressRepository.findByUsernameAndBookId(progress.username, bookId).asScala.foreach(p => progress.id = p.id)
        progressRepository.save(progress)
        true

      case _ =>
        false
    }
  }

  def deleteProgress(bookId: String) = {
    progressRepository
      .findByUsernameAndBookId(SessionUtil.getUser().username, bookId).asScala
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
}

