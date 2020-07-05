package com.cacoveanu.reader.service

import com.cacoveanu.reader.entity.Account
import com.cacoveanu.reader.repository.{BookRepository, ProgressRepository}
import com.cacoveanu.reader.util.EpubUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import scala.jdk.CollectionConverters._
import com.cacoveanu.reader.util.OptionalUtil.AugmentedOptional

import scala.beans.BeanProperty

@Service
class BookService {

  @BeanProperty
  @Autowired
  var bookRepository: BookRepository = _

  @BeanProperty
  @Autowired
  var progressRepository: ProgressRepository = _

  def loadBookWithProgress(account: Account, id: String) = {
    bookRepository.findById(id).asScala match {
      case Some(book) =>
        progressRepository.findByUserAndBook(account, book).asScala match {
          case Some(progress) => Some((book, progress.section, progress.position))
          case None =>
            val firstResource = EpubUtil.getToc(book.path).headOption.map(t => t.link).getOrElse("")
            Some((book, firstResource, 0))
        }
      case None => None
    }
  }

}
