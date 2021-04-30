package com.cacoveanu.reader.service

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import com.cacoveanu.reader.util.FileUtil

import javax.imageio.ImageIO
import org.springframework.stereotype.Service

import java.awt
import java.awt.Color
import scala.collection.mutable
import java.awt.Font
import java.awt.FontFormatException
import java.awt.GraphicsEnvironment
import java.io.IOException
import scala.util.Random

@Service
class ImageService {

  // install fonts
  try {
    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment
    ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, this.getClass.getResourceAsStream("/static/Merriweather/Merriweather-Regular.ttf")))
  } catch {
    case e: Throwable =>
      e.printStackTrace()
  }

  private def hex2Rgb(colorStr: String) =
    Color(
      Integer.valueOf(colorStr.substring(1, 3), 16),
      Integer.valueOf(colorStr.substring(3, 5), 16),
      Integer.valueOf(colorStr.substring(5, 7), 16)
    )

  private def toGraphicsColor(color: Color) = new awt.Color(color.red, color.green, color.blue)

  private def pickAccentColor(): Color = {
    val options = Seq(hex2Rgb("#FEC5BB"),
      hex2Rgb("#9bf6ff"), hex2Rgb("#a0c4ff"), hex2Rgb("#caffbf"), hex2Rgb("#fdffb6"),
      hex2Rgb("#ffadad"), hex2Rgb("#ccd5ae"), hex2Rgb("#d4a373"))
    options(Random.nextInt(options.size))
  }

  def generateCover(title: String, accentColor: Color = pickAccentColor()): Array[Byte] = {
    val height = 700
    val width = (height * .6).toInt
    val margin = (height * .05).toInt

    val generatedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val graphics = generatedImage.createGraphics
    val serifFont = new Font("Merriweather", Font.PLAIN, 50)
    graphics.setFont(serifFont)
    val serifFontMetrics = graphics.getFontMetrics(serifFont)

    var lines = Seq[String]()
    val tokens = title.split("\\s")
    var currentLine = ""
    for (token <- tokens) {
      // add token to line
      if (currentLine.length > 0) currentLine = currentLine + " "
      currentLine = currentLine + token
      // check size of line
      if (serifFontMetrics.stringWidth(currentLine) > width - 4 * margin) {
        // remove last token
        currentLine = currentLine.substring(0, currentLine.lastIndexOf(" "))
        // save line
        lines = lines :+ currentLine
        // generate new line
        currentLine = token
      }
    }
    if (currentLine.length > 0) {
      lines = lines :+ currentLine
    }

    // draw lines
    var lineHeight = margin
    for (line <- lines) {
      graphics.drawString(line, margin*2, lineHeight + serifFontMetrics.getHeight())
      lineHeight = lineHeight + serifFontMetrics.getHeight()
    }

    // draw rectangles over edges to hide "overflow"
    graphics.setColor(toGraphicsColor(accentColor))
    graphics.fillRect(0, 0, margin, height)
    graphics.fillRect(0, 0, width, margin)
    graphics.fillRect(0, height-margin, width, margin)
    graphics.fillRect(width-margin, 0, margin, height)

    graphics.dispose()

    //ImageIO.write(generatedImage, "jpeg", new File("test.jpg"))

    val out = new ByteArrayOutputStream()
    ImageIO.write(generatedImage, "jpeg", out)
    out.toByteArray
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