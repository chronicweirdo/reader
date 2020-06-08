package com.cacoveanu.reader.service

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.cacoveanu.reader.util.FileUtil
import javax.activation.MimeType
import javax.imageio.ImageIO
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

@Service
class ImageService {

  private val IMAGE_BMP_VALUE = "image/bmp"

  def isImageType(fileName: String) =
    Seq("jpg", "jpeg", "png", "gif", "bmp") contains FileUtil.getExtension(fileName)

  def getMediaType(formatName: String): Option[String] =
    FileUtil.getExtension(formatName) match {
      case "jpg" => Some(MediaType.IMAGE_JPEG_VALUE)
      case "jpeg" => Some(MediaType.IMAGE_JPEG_VALUE)
      case "png" => Some(MediaType.IMAGE_PNG_VALUE)
      case "gif" => Some(MediaType.IMAGE_GIF_VALUE)
      case "bmp" => Some(IMAGE_BMP_VALUE)
      case _ => None
    }

  def getFormatName(mediaType: String): Option[String] =
    mediaType match {
      case MediaType.IMAGE_GIF_VALUE => Some("gif")
      case MediaType.IMAGE_JPEG_VALUE => Some("jpeg")
      case MediaType.IMAGE_PNG_VALUE => Some("png")
      case IMAGE_BMP_VALUE => Some("bmp")
      case _ => None
    }

  private def resizeByFactor(factor: Double, originalWidth: Int, originalHeight: Int) =
    ((originalWidth * factor).floor.toInt, (originalHeight * factor).floor.toInt)

  private def resizeByMinimalSide(minimalSideSize: Int, originalWidth: Int, originalHeight: Int) =
    if (originalWidth < originalHeight) {
      val newWidth = minimalSideSize
      val newHeight = ((newWidth.toDouble / originalWidth) * originalHeight).floor.toInt
      (newWidth, newHeight)
    } else {
      val newHeight = minimalSideSize
      val newWidth = ((newHeight.toDouble / originalHeight) * originalWidth).floor.toInt
      (newWidth, newHeight)
    }

  private def resize(original: Array[Byte], formatName: String, strategy: (Int, Int) => (Int, Int)): Array[Byte] = {
    val image = ImageIO.read(new ByteArrayInputStream(original))
    val (newWidth, newHeight) = strategy(image.getWidth, image.getHeight)
    val resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
    val g = resized.createGraphics
    g.drawImage(image, 0, 0, newWidth, newHeight, null)
    g.dispose()

    val out = new ByteArrayOutputStream()
    ImageIO.write(resized, formatName, out)

    out.toByteArray
  }

  def resizeImageByFactor(original: Array[Byte], formatName: String, factor: Double): Array[Byte] =
    resize(original, formatName, resizeByFactor(factor, _: Int, _: Int))

  def resizeImageByMinimalSide(original: Array[Byte], formatName: String, minimalSide: Int): Array[Byte] =
    resize(original, formatName, resizeByMinimalSide(minimalSide, _: Int, _: Int))
}
