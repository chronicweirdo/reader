package com.cacoveanu.reader.service

import com.cacoveanu.reader.entity.Content
import com.cacoveanu.reader.repository.BookRepository
import com.cacoveanu.reader.util.{CbrUtil, CbzUtil, FileTypes, FileUtil}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._
import com.cacoveanu.reader.util.OptionalUtil.AugmentedOptional

@Service
class ContentService {

  private val COMIC_PART_SIZE = 20

  @BeanProperty
  @Autowired
  var bookRepository: BookRepository = _

  @Cacheable(Array("comicParts"))
  def loadComicPart(id: String, part: Int): Seq[Content] = {
    bookRepository.findById(id).asScala match {
      case Some(book) =>
        FileUtil.getExtension(book.path) match {
          case FileTypes.CBR => CbrUtil.readPages(book.path, Some(computePagesForPart(part))).getOrElse(Seq())
          case FileTypes.CBZ => CbzUtil.readPages(book.path, Some(computePagesForPart(part))).getOrElse(Seq())
          case _ => Seq()
        }
      case None => Seq()
    }
  }

  def computePartNumberForPage(page: Int) = {
    page / COMIC_PART_SIZE
  }

  private def computePagesForPart(part: Int) = {
    (part * COMIC_PART_SIZE) until (part * COMIC_PART_SIZE + COMIC_PART_SIZE)
  }
}
