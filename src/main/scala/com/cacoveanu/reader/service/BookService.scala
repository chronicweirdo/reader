package com.cacoveanu.reader.service

import com.cacoveanu.reader.entity.{Account, Book, Progress}
import com.cacoveanu.reader.repository.{BookRepository, ProgressRepository}
import com.cacoveanu.reader.util.{EpubUtil, FileTypes, FileUtil}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import scala.jdk.CollectionConverters._
import com.cacoveanu.reader.util.OptionalUtil.AugmentedOptional
import org.springframework.data.domain.{PageRequest, Sort}
import org.springframework.data.domain.Sort.Direction

import scala.beans.BeanProperty
import scala.util.matching.Regex

@Service
class BookService {

  private val PAGE_SIZE = 20

  @BeanProperty
  @Autowired
  var bookRepository: BookRepository = _

  @BeanProperty
  @Autowired
  var progressRepository: ProgressRepository = _

  def loadBook(id: String) = {
    bookRepository.findById(id).asScala
  }

  def bookType(book: Book) = {
    FileUtil.getExtension(book.path)
  }

  // todo: rethink this whole shit; a book id and a position should be enought to know what to load
  def loadBookWithProgress(account: Account, id: String) = {
    bookRepository.findById(id).asScala match {
      case Some(book) =>
        progressRepository.findByUserAndBook(account, book).asScala match {
          case Some(progress) => Some((book, getSectionForPosition(book, progress.position).getOrElse(""), progress.position))
          case None => getNewProgressForBook(book)
        }
      case None => None
    }
  }

  private def getSectionForPosition(book: Book, position: Int) = {
    bookType(book) match {
      case FileTypes.EPUB =>
        val toc = EpubUtil.getToc(book.path)
        var currentSize = 0
        toc.map(e => {
          currentSize = currentSize + e.size
          (currentSize, e)
        }).find(e => e._1 > position).map(e => EpubUtil.baseLink(e._2.link))
      case _ => None
    }
  }

  private def getNewProgressForBook(book: Book) = {
    bookType(book) match {
      case FileTypes.CBR => Some((book, "", 0))
      case FileTypes.CBZ => Some((book, "", 0))
      case FileTypes.EPUB =>
        val firstResource = EpubUtil.getToc(book.path).headOption.map(t => t.link).getOrElse("")
        Some((book, firstResource, 0))
      case _ => None
    }
  }

  /* progress code */

  def loadProgress(user: Account, book: Book): Option[Progress] = {
    progressRepository.findByUserAndBook(user, book).asScala
  }

  def loadProgress(user: Account, books: Seq[Book]): Seq[Progress] = {
    progressRepository.findByUserAndBookIn(user, books.asJava).asScala.toSeq
  }

  def loadTopProgress(user: Account, limit: Int): Seq[Progress] = {
    val sort = Sort.by(Direction.DESC, "last_update")
    val pageRequest = PageRequest.of(0, limit, sort)
    progressRepository.findUnreadByUser(user, pageRequest).asScala.toSeq
  }

  def saveProgress(progress: Progress) = {
    progressRepository.findByUserAndBook(progress.user, progress.book).asScala.foreach(p => progress.id = p.id)
    progressRepository.save(progress)
  }

  def deleteProgress(progress: Progress) = {
    progressRepository.delete(progress)
  }

  /* collections code */

  def loadCollections(): Seq[String] = {
    bookRepository.findAllCollections().asScala.toSeq
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
}
