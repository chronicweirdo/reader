package com.cacoveanu.reader.util

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import com.cacoveanu.reader.entity.Content

import javax.imageio.ImageIO
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.slf4j.{Logger, LoggerFactory}

object PdfUtil {

  private val log: Logger = LoggerFactory.getLogger(PdfUtil.getClass)

  def countPages(path: String): Option[Int] = {
    try {
      val document = PDDocument.load(new File(path))
      Some(document.getNumberOfPages())
    } catch {
      case e: Throwable =>
        log.warn(s"can't read pdf $path", e)
        None
    }
  }

  def countPages2(fileBytes: Array[Byte]): Option[Int] = {
    try {
      val document = PDDocument.load(new ByteArrayInputStream(fileBytes))
      Some(document.getNumberOfPages())
    } catch {
      case e: Throwable =>
        //log.warn(s"can't read pdf $path", e) // todo: fix error
        None
    }
  }

  def readPages(path: String, pages: Option[Seq[Int]] = None): Option[Seq[Content]] = {
    try {
      val document = PDDocument.load(new File(path))
      val renderer = new PDFRenderer(document)
      val numberOfPages = document.getNumberOfPages()
      pages match {
        case Some(pgs) => Some(pgs.filter(p => p >= 0 && p < numberOfPages)
          .map(p => {
            val image: BufferedImage = renderer.renderImage(p, 2)
            val out = new ByteArrayOutputStream()
            ImageIO.write(image, "png", out)
            //ImageIO.write(image, "jpeg", out)
            val arr = out.toByteArray
            Content(Some(p), FileMediaTypes.IMAGE_JPEG_VALUE, arr)
          }))
        case None => None
      }
    } catch {
      case e: Throwable =>
        log.warn(s"can't read pdf $path", e)
        None
    }
  }

  def readPages2(fileBytes: Array[Byte], pages: Option[Seq[Int]] = None): Option[Seq[Content]] = {
    try {
      val document = PDDocument.load(new ByteArrayInputStream(fileBytes))
      val renderer = new PDFRenderer(document)
      val numberOfPages = document.getNumberOfPages()
      pages match {
        case Some(pgs) => Some(pgs.filter(p => p >= 0 && p < numberOfPages)
          .map(p => {
            val image: BufferedImage = renderer.renderImage(p, 2)
            val out = new ByteArrayOutputStream()
            ImageIO.write(image, "png", out)
            //ImageIO.write(image, "jpeg", out)
            val arr = out.toByteArray
            Content(Some(p), FileMediaTypes.IMAGE_JPEG_VALUE, arr)
          }))
        case None => None
      }
    } catch {
      case e: Throwable =>
        //log.warn(s"can't read pdf $path", e) todo: fix error
        None
    }
  }
}
