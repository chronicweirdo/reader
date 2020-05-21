package com.cacoveanu.reader.service

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.cacoveanu.reader.util.FileUtil
import javax.imageio.ImageIO
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

@Service
class ImageService {

  def getMediaType(formatName: String): Option[MediaType] =
    FileUtil.getExtension(formatName) match {
      case "jpg" => Some(MediaType.IMAGE_JPEG)
      case "jpeg" => Some(MediaType.IMAGE_JPEG)
      case "png" => Some(MediaType.IMAGE_PNG)
      case "gif" => Some(MediaType.IMAGE_GIF)
      case _ => None
    }

  def getFormatName(mediaType: MediaType): Option[String] =
    mediaType match {
      case MediaType.IMAGE_GIF => Some("gif")
      case MediaType.IMAGE_JPEG => Some("jpeg")
      case MediaType.IMAGE_PNG => Some("png")
      case _ => None
    }

  def resizeImage(original: Array[Byte], formatName: String, factor: Double): Array[Byte] = {
    val image = ImageIO.read(new ByteArrayInputStream(original))
    val originalHeight = image.getHeight
    val originalWidth = image.getWidth
    val newHeight = (originalHeight * factor).floor.toInt
    val newWidth = (originalWidth * factor).floor.toInt

    val resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
    val g = resized.createGraphics
    g.drawImage(image, 0, 0, newWidth, newHeight, null)
    g.dispose()

    val out = new ByteArrayOutputStream()
    ImageIO.write(resized, formatName, out)

    out.toByteArray
  }
}
