package com.cacoveanu.reader.util

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer

import java.awt.image.BufferedImage
import java.io.{ByteArrayOutputStream, File, FileOutputStream}
import javax.imageio.ImageIO

object TestPdfRendering {

  def writeOut(image: BufferedImage, name: String) = {
    val out = new FileOutputStream(name)
    ImageIO.write(image, "png", out)
    out.close()
  }

  def main(args: Array[String]): Unit = {
    if (args.length < 2) throw new Exception("not enough arguments")
    val filepath = args(0)
    val page = args(1).toInt
    println(s"rendering: $filepath page $page")

    val document = PDDocument.load(new File(filepath))
    val renderer = new PDFRenderer(document)

    val image0: BufferedImage = renderer.renderImageWithDPI(page, 600f)
    writeOut(image0, "image0.png")

    val image1: BufferedImage = renderer.renderImage(page)
    writeOut(image1, "image1.png")

    val image2: BufferedImage = renderer.renderImage(page, 2)
    writeOut(image2, "image2.png")

    val image3: BufferedImage = renderer.renderImageWithDPI(page, 300)
    writeOut(image3, "image3.png")
  }

}
