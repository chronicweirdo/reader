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
    EpubUtil.getToc2(bookPath)
  }
}
