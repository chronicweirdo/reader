package com.cacoveanu.reader.service

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Paths}

import com.cacoveanu.reader.util.FileMediaTypes
import javax.imageio.ImageIO
import org.junit.jupiter.api.Test

class ImageServiceTest {

  private def getDimensions(bytes: Array[Byte]) = {
    val image = ImageIO.read(new ByteArrayInputStream(bytes))
    (image.getWidth, image.getHeight)
  }

  @Test
  def testImageResizeByFactor() = {
    val path = Paths.get("src", "main", "resources", "static", "gold_logo.png") // 198 x 154
    val image = Files.readAllBytes(path)
    val (originalWidth, originalHeight) = getDimensions(image)

    val service = new ImageService()
    val resizedImage = service.resizeImageByFactor(image, FileMediaTypes.IMAGE_PNG_VALUE, .5)
    val (resizedWidth, resizedHeight) = getDimensions(resizedImage)

    assert(Math.abs(originalWidth.toDouble - resizedWidth.toDouble * 2) < 1.0)
    assert(Math.abs(originalHeight.toDouble - resizedHeight.toDouble * 2) < 1.0)
  }

  @Test
  def testImageResizeByMinimalSide() = {
    val path = Paths.get("src", "main", "resources", "static", "gold_logo.png") // 198 x 154
    val image = Files.readAllBytes(path)
    val (originalWidth, originalHeight) = getDimensions(image)

    val service = new ImageService()
    val resizedImage = service.resizeImageByMinimalSide(image, FileMediaTypes.IMAGE_PNG_VALUE, 100)
    val (resizedWidth, resizedHeight) = getDimensions(resizedImage)

    assert(resizedHeight == 100)
    assert(Math.abs(resizedWidth.toDouble / resizedHeight - originalWidth.toDouble / originalHeight) < 0.01)
  }
}
