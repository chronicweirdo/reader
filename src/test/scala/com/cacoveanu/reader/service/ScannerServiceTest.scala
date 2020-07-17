package com.cacoveanu.reader.service

import java.nio.file.Paths

import org.junit.jupiter.api.Test

class ScannerServiceTest {

  @Test
  def testScan() = {
    val path = "d:\\books\\2020.06 books\\"
    val path1 = Paths.get("src", "test", "resources", "rembrandt.epub").toString

    val service = new ScannerService()
    //service.libraryLocation = "d:\\books"
    service.libraryLocation = Paths.get("src", "test", "resources").toString
    service.imageService = new ImageService
    val book = service.scanEpub(path1)
    println(book)
  }
}
