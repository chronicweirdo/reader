package com.cacoveanu.reader.service

import com.cacoveanu.reader.util.EpubUtil
import org.junit.jupiter.api.Test

class ScannerServiceTest {

  val service = new ScannerService
  service.libraryLocation = "."
  service.imageService = new ImageService

  @Test
  def testScanEpub(): Unit = {
    val bookPath = ".\\book4.epub"
    //val book = service.scanEpub(bookPath)
    //println(book)
    val (resources, links, toc) = EpubUtil.scanContentMetadata(bookPath)
    println("book resources:")
    resources.foreach(r => println(r.start + "," + r.end + "," + r.path))
    println("\nbook links:")
    links.foreach(l => println(l.link + "," + l.position))
    println("\n book toc:")
    toc.foreach(t => println(t.index + "," + t.title + "," + t.position))
  }
}
