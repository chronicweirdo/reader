package com.cacoveanu.reader.service

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, FileOutputStream, OutputStream}
import java.awt.Graphics2D
import java.awt.image.BufferedImage

import javax.imageio.ImageIO
import org.junit.jupiter.api.Test

class ImageResize {

  val path = "C:\\Users\\silvi\\Dropbox\\comics\\Avatar The Legend Of Korra\\The Legend of Korra - Turf Wars (001-003)(2017-2018)(digital)(Raven)\\The Legend of Korra - Turf Wars - Part 1 (2017) (Digital) (Raven).cbz"
  val comicService = new ComicService

  @Test def comicServiceShouldLoadImageBytes() {
    val page = comicService.readPage(path, 0)

    assert(page.isDefined)
    assert(page.get.data.length > 0)
    println("page length: " + page.get.data.length)
  }

  @Test def saveImageOutside(): Unit = {
    val page = comicService.readPage(path, 0)

    page match {
      case Some(p) =>
        try {
          val outputStream = new FileOutputStream("test.jpg")
          try outputStream.write(p.data)
          finally if (outputStream != null) outputStream.close()
        }
    }
  }

  @Test def resizeImageAndSave(): Unit = {
    val page = comicService.readPage(path, 0)

    page match {
      case Some(p) =>
        val image = ImageIO.read(new ByteArrayInputStream(p.data))
        val originalHeight = image.getHeight
        val originalWidth = image.getWidth
        val newHeight = (originalHeight * .1).floor.toInt
        val newWidth = (originalWidth * .1).floor.toInt

        val resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
        val g = resized.createGraphics
        g.drawImage(image, 0, 0, newWidth, newHeight, null)
        g.dispose()

        val out = new ByteArrayOutputStream()
        ImageIO.write(resized, "jpg", out)
        val outBytes = out.toByteArray

        try {
          val outputStream = new FileOutputStream("resized2.jpg")
          try outputStream.write(outBytes)
          finally if (outputStream != null) outputStream.close()
        }
    }
  }
}
