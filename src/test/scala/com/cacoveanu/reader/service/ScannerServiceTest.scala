package com.cacoveanu.reader.service

import org.junit.jupiter.api.Test

class ScannerServiceTest {

  val service = new ScannerService
  service.libraryLocation = "."
  service.imageService = new ImageService

  @Test
  def testScanEpub(): Unit = {
    val book = service.scanEpub(".\\book1.epub")
    println(book)
  }
}
