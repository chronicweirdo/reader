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

  /*def getFormatName(mediaType: String): Option[String] =
    mediaType match {
      case MediaType.IMAGE_GIF_VALUE => Some("gif")
      case MediaType.IMAGE_JPEG_VALUE => Some("jpeg")
      case MediaType.IMAGE_PNG_VALUE => Some("png")
      case IMAGE_BMP_VALUE => Some("bmp")
      case _ => None
    }*/

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

  private def resize(original: Array[Byte], mediaType: String, strategy: (Int, Int) => (Int, Int)): Array[Byte] = {
    val image = ImageIO.read(new ByteArrayInputStream(original))
    val (newWidth, newHeight) = strategy(image.getWidth, image.getHeight)
    val resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
    val g = resized.createGraphics
    g.drawImage(image, 0, 0, newWidth, newHeight, null)
    g.dispose()

    val out = new ByteArrayOutputStream()
    FileUtil.getExtensionForMediaType(mediaType) match {
      case Some(formatName) => ImageIO.write(resized, formatName, out)
    }
    out.toByteArray
  }

  def resizeImageByFactor(original: Array[Byte], mediaType: String, factor: Double): Array[Byte] =
    resize(original, mediaType, resizeByFactor(factor, _: Int, _: Int))

  def resizeImageByMinimalSide(original: Array[Byte], mediaType: String, minimalSide: Int): Array[Byte] =
    resize(original, mediaType, resizeByMinimalSide(minimalSide, _: Int, _: Int))
}
