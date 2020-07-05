package com.cacoveanu.reader.service

import com.cacoveanu.reader.entity.{Account, Book, Progress}
import com.cacoveanu.reader.repository.{BookRepository, ProgressRepository}
import com.cacoveanu.reader.util.{EpubUtil, FileTypes, FileUtil}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import scala.jdk.CollectionConverters._
import com.cacoveanu.reader.util.OptionalUtil.AugmentedOptional
import org.apache.commons.vfs2.FileType
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

  def loadBookWithProgress(account: Account, id: String) = {
    bookRepository.findById(id).asScala match {
      case Some(book) =>
        progressRepository.findByUserAndBook(account, book).asScala match {
          case Some(progress) => Some((book, progress.section, progress.position))
          case None => getNewProgressForBook(book)
        }
      case None => None
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
