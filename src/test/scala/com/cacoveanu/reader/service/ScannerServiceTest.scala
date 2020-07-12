package com.cacoveanu.reader.service

import org.junit.jupiter.api.Test

class ScannerServiceTest {

  @Test
  def testScan() = {
    val path = "d:\\books"

    val service = new ScannerService()
    service.libraryLocation = "d:\\books"
    service.imageService = new ImageService
    val book = service.scanEpub("x", path)
    println(book)
  }
}
