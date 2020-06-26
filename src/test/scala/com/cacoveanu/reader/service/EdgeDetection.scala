package com.cacoveanu.reader.service

import java.awt.Graphics
import java.awt.image.{BufferedImage, BufferedImageOp, ConvolveOp, Kernel}
import java.io.{BufferedOutputStream, ByteArrayInputStream, ByteArrayOutputStream, FileOutputStream}
import java.nio.file.{Files, Paths}
import java.awt.Graphics2D

import javax.imageio.ImageIO
import jcanny.JCanny

trait MyFilter {
  def process(image: BufferedImage): BufferedImage
}

class BlurFilter extends MyFilter {
  override def process(image: BufferedImage): BufferedImage = {
    val blurMatrix = Array(5.0f / 9.0f, 5.0f / 9.0f, 5.0f / 9.0f, 5.0f / 9.0f, 5.0f / 9.0f, 5.0f / 9.0f, 5.0f / 9.0f, 5.0f / 9.0f, 5.0f / 9.0f)
    val blurFilter: ConvolveOp = new ConvolveOp(new Kernel(3, 3, blurMatrix), ConvolveOp.EDGE_NO_OP, null);
    blurFilter.filter(image, null)
  }
}

class GaussianFilter extends MyFilter {
  override def process(image: BufferedImage): BufferedImage = {
    val matrix = Array(
      2f/159, 4f/159, 5f/159, 4f/159, 2f/159,
      4f/159, 9f/159, 12f/159, 9f/159, 4f/159,
      5f/159, 12f/159, 15f/159, 12f/159, 5f/159,
      4f/159, 9f/159, 12f/159, 9f/159, 4f/159,
      2f/159, 4f/159, 5f/159, 4f/159, 2f/159
    )
    val filter = new ConvolveOp(new Kernel(5, 5, matrix), ConvolveOp.EDGE_NO_OP, null)
    filter.filter(image, null)
  }
}

class CustomFilter(width: Int, height: Int, matrix: Array[Float]) extends MyFilter {
  override def process(image: BufferedImage): BufferedImage = {
    val filter = new ConvolveOp(new Kernel(width, height, matrix), ConvolveOp.EDGE_NO_OP, null)
    filter.filter(image, null)
  }
}

object EdgeDetection {

  def loadImageBytes(path: String) = Files.readAllBytes(Paths.get(path))

  def loadImage(path: String) = ImageIO.read(new ByteArrayInputStream(loadImageBytes(path)))

  def imageToBytes(image: BufferedImage): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    ImageIO.write(image, "jpg", out)
    out.toByteArray
  }

  def bytesToFile(bytes: Array[Byte], path: String) = {
    val bos = new BufferedOutputStream(new FileOutputStream(path))
    bos.write(bytes)
    bos.close()
  }

  def imageToFile(image: BufferedImage, path: String) = {
    bytesToFile(imageToBytes(image), path)
  }

  def toGrayscale(img: BufferedImage): BufferedImage = {
    val out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY)
    val g = out.createGraphics
    g.drawImage(img, 0, 0, null)
    out
  }

  def toBW(img: BufferedImage): BufferedImage = {
    val out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_BINARY)
    val g = out.createGraphics
    g.drawImage(img, 0, 0, null)
    out
  }

  /*def merge(img1: BufferedImage, img2: BufferedImage): BufferedImage = {
    val out = new BufferedImage(
      Math.min(img1.getWidth(), img2.getWidth()),
      Math.min(img2.getHeight(), img2.getHeight()),
      BufferedImage.TYPE_INT_RGB
    )
    val g = out.createGraphics
    g.drawImage(img1, 0, 0, null)
    g.drawImage(img2, 0, 0, null)
    out
  }*/

  def rgb(r: Int, g: Int, b: Int, a: Int = 255) = ((a&0x0ff)<<24)|((r&0x0ff)<<16)|((g&0x0ff)<<8)|(b&0x0ff)

  def threshold(img: BufferedImage, t: Int): BufferedImage = {
    val out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY)
    for (x <- 0 until img.getWidth()) {
      for (y <- 0 until img.getHeight()) {
        //println(img.getRGB(x, y))
        if (img.getRGB(x, y) >= t) {
          out.setRGB(x, y, img.getRGB(x, y))
        } else {
          out.setRGB(x, y, 0)
        }
      }
    }

    out
  }

  def combine(img1: BufferedImage, img2: BufferedImage): BufferedImage = {
    val out = new BufferedImage(
      Math.min(img1.getWidth(), img2.getWidth()),
      Math.min(img2.getHeight(), img2.getHeight()),
      BufferedImage.TYPE_INT_RGB
    )
    for (x <- 0 until out.getWidth) {
      for (y <- 0 until out.getHeight) {
        val p1 = img1.getRGB(x, y)
        val p2 = img2.getRGB(x, y)
        if (p1 == p2) {
          out.setRGB(x, y, p1)
        }
      }
    }

    out
  }

  def canny(img: BufferedImage): BufferedImage = {
    val sobelHorizontal = new CustomFilter(3, 3, Array(1f, 2f, 1f, 0, 0, 0, -1f, -2f, -1f))
    val gx = sobelHorizontal.process(img)
    val sobelVertical = new CustomFilter(3, 3, Array(1f, 0, -1, 2, 0, -2, 1, 0, -1))
    val gy = sobelVertical.process(img)

    // compute edge gradient values
    val gradient = Array[Double](img.getHeight() * img.getWidth())
    val edgeDirection = Array[Int](img.getHeight() * img.getWidth())
    for (y <- 1 until img.getHeight - 1) {
      for (x <- 1 until img.getWidth - 1) {
        gradient(img.getWidth * y + x) = Math.sqrt(Math.pow(gx.getRGB(x, y), 2) + Math.pow(gy.getRGB(x, y), 2))
        val angle = (Math.atan2(gy.getRGB(x, y), gx.getRGB(x, y)) / Math.PI) * 180.0

        /* Convert actual edge direction to approximate value *//* Convert actual edge direction to approximate value */
        val newAngle = if (((angle < 22.5) && (angle > -22.5)) || (angle > 157.5) || (angle < -157.5)) 0
          else if (((angle > 22.5) && (angle < 67.5)) || ((angle < -112.5) && (angle > -157.5))) 45
          else if (((angle > 67.5) && (angle < 112.5)) || ((angle < -67.5) && (angle > -112.5))) 90
          else if (((angle > 112.5) && (angle < 157.5)) || ((angle < -22.5) && (angle > -67.5))) 135
          else 0
        edgeDirection(img.getWidth * y + x) = newAngle
      }
    }

    null

  }

  def main(args: Array[String]): Unit = {
    val inputImagePath = "input.jpg"

    val image: BufferedImage = loadImage(inputImagePath)
    val grayImage = toGrayscale(image)
    val blackImage = toBW(image)
    val hFilter = new CustomFilter(3, 1, Array(-1f, 0, 1))
    var hEdges = grayImage
    //for (i <- 0 to 5)
    hEdges = hFilter.process(hEdges)
    hEdges = threshold(hEdges, rgb(220, 220, 220))
    imageToFile(hEdges, "h.jpg")

    /*for (x <- 0 to (image.getWidth / 4)) {
      for (y <- 0 to (image.getHeight / 4)) {
        image.setRGB(x, y, 0)
      }
    }*/

    /*val gaussianFilter = new GaussianFilter
    val blurredImage = gaussianFilter.process(image)
    val bwImage = toBW(blurredImage)

    imageToFile(bwImage, "bw.jpg")
    val sobelHorizontal = new CustomFilter(3, 3, Array(1f, 2f, 1f, 0, 0, 0, -1f, -2f, -1f))
    val s1 = sobelHorizontal.process(bwImage)
    val sobelSelect = new CustomFilter(3, 3, Array(1f, 2f, 1f, 0, 0, 0, -1f, -2f, -1f))
    val s11 = sobelSelect.process(s1)
    imageToFile(s11, "s11.jpg")


    imageToFile(s1, "s1.jpg")
    val sobelVertical = new CustomFilter(3, 3, Array(1f, 0, -1, 2, 0, -2, 1, 0, -1))
    val s2 = sobelVertical.process(bwImage)
    imageToFile(s2, "s2.jpg")

    val s = combine(s1, s2)
    imageToFile(s, "s.jpg")*/

    //val g: Graphics = image.getGraphics

    //out.toByteArray

    /*val outputFilePath = "test.jpg"
    bytesToFile(imageToBytes(blurredImage), outputFilePath)*/
  }

}