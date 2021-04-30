package com.cacoveanu.reader.util

import jdk.internal.platform.Container.metrics
import org.junit.jupiter.api.Test

import java.awt.{Color, Font}
import java.awt.image.BufferedImage
import java.io.{ByteArrayOutputStream, File}
import javax.imageio.ImageIO

class TestGenerateCover {

  @Test
  def testGenerateCover(): Unit = {
    val title = "A Long Forgotten Book Title for a Book Without a Cover"

    import java.awt.Font
    import java.awt.FontFormatException
    import java.awt.GraphicsEnvironment
    import java.io.IOException
    var ge: GraphicsEnvironment = null
    try {
      ge = GraphicsEnvironment.getLocalGraphicsEnvironment
      ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, this.getClass.getResourceAsStream("/static/Merriweather/Merriweather-Regular.ttf")))
    } catch {
      case e: FontFormatException =>
        e.printStackTrace()
      case e: IOException =>
        e.printStackTrace()
    }


    val height = 700
    val width = (height * .6).toInt
    val margin = (height * .05).toInt //20
    val resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = resized.createGraphics
    val serifFont = new Font("Merriweather", Font.PLAIN, 50)
    g.setFont(serifFont)
    // get metrics from the graphics
    val serifFontMetrics = g.getFontMetrics(serifFont)

    var lines = Seq[String]()
    var tokens = title.split("\\s")
    var currentLine = ""
    for (token <- tokens) {
      // add token to line
      if (currentLine.length > 0) currentLine = currentLine + " "
      currentLine = currentLine + token
      // check size of line
      if (serifFontMetrics.stringWidth(currentLine) > width - 2 * margin) {
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
      g.drawString(line, margin, lineHeight + serifFontMetrics.getHeight())
      lineHeight = lineHeight + serifFontMetrics.getHeight()
    }

    // draw rectangles over edges to hide "overflow"
    g.setColor(Color.red)
    g.fillRect(0, 0, margin, height)
    g.fillRect(0, 0, width, margin)
    g.fillRect(0, height-margin, width, margin)
    g.fillRect(width-margin, 0, margin, height)

    g.dispose()

    /*val out = new ByteArrayOutputStream()
    FileUtil.getExtensionForMediaType(mediaType) match {
      case Some(formatName) => ImageIO.write(resized, formatName, out)
    }
    out.toByteArray*/
    ImageIO.write(resized, "jpeg", new File("test.jpg"))
  }
}
