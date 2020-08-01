package com.cacoveanu.reader

import java.awt.image.BufferedImage
import java.io.{BufferedOutputStream, ByteArrayOutputStream, File, FileOutputStream}

import javax.imageio.ImageIO
import org.junit.jupiter.api.Test
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper

class TestReadPdf {

  @Test
  def tst() = {
    val document = PDDocument.load(new File(""))

    println(document.getNumberOfPages())
    //val page = document.getPage(10)

    val renderer = new PDFRenderer(document)
    val image: BufferedImage = renderer.renderImage(101)

    val out = new ByteArrayOutputStream()
    ImageIO.write(image, "png", out)
    val arr = out.toByteArray
    println(arr)

    val bos = new BufferedOutputStream(new FileOutputStream("page10.png"))
    bos.write(arr)
    bos.close()

    /*if (!document.isEncrypted) {
      val stripper = new PDFTextStripper
      val text = stripper.getText(document)
      System.out.println("Text:" + text)
    }*/
  }
}
