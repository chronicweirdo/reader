package com.cacoveanu.reader.service

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.cacoveanu.reader.util.FileUtil
import javax.imageio.ImageIO
import org.springframework.stereotype.Service

import scala.collection.mutable

@Service
class ImageService {

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

  private def getColor(rgb: Int) = Color(
    (rgb & 0x00ff0000) >> 16,
    (rgb & 0x0000ff00) >> 8,
    (rgb & 0x000000ff)
  )

  private def increaseInMap(edgeMap: mutable.Map[Color, Int], c: Color) =
    edgeMap.put(c, edgeMap.getOrElse(c, 0) + 1)

  def getDominantEdgeColor(imageArray: Array[Byte]): Array[Int] = {
    val image = ImageIO.read(new ByteArrayInputStream(imageArray))
    val edgeMap = mutable.Map[Color, Int]()
    for (y <- 0 until image.getHeight) {
      val c1 = getColor(image.getRGB(0, y))
      val c2 = getColor(image.getRGB(image.getWidth - 1, y))
      increaseInMap(edgeMap, c1)
      increaseInMap(edgeMap, c2)
    }
    for (x <- 1 until image.getWidth - 1) {
      val c1 = getColor(image.getRGB(x, 0))
      val c2 = getColor(image.getRGB(x, image.getHeight - 1))
      increaseInMap(edgeMap, c1)
      increaseInMap(edgeMap, c2)
    }
    val colorsRepresentation = edgeMap.toSeq.sortBy(_._2).reverse
    val mostRepresented = colorsRepresentation.head
    val selectedColors = colorsRepresentation.filter(e => e._2 / mostRepresented._2 > .1)
    val resultColor = selectedColors.foldLeft(Color(0, 0, 0))(_ + _._1) / selectedColors.size
    Array(resultColor.red, resultColor.green, resultColor.blue)
  }
}

case class Color(red: Int, green: Int, blue: Int) {
  def + (c: Color): Color = Color(red + c.red, green + c.green, blue + c.blue)
  def / (n: Int): Color = Color(Math.floor(red / n).toInt, Math.floor(green / n).toInt, Math.floor(blue / n).toInt)
}