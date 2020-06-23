package com.cacoveanu.reader.service

import java.awt.Graphics
import java.awt.image.{BufferedImage, BufferedImageOp, ConvolveOp, Kernel}
import java.io.{BufferedOutputStream, ByteArrayInputStream, ByteArrayOutputStream, FileOutputStream}
import java.nio.file.{Files, Paths}
import java.awt.Graphics2D

import javax.imageio.ImageIO

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

  def toBW(img: BufferedImage): BufferedImage = {
    val out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY)
    val g = out.createGraphics
    g.drawImage(img, 0, 0, null)
    out
  }

  def merge(img1: BufferedImage, img2: BufferedImage): BufferedImage = {
    val out = new BufferedImage(
      Math.min(img1.getWidth(), img2.getWidth()),
      Math.min(img2.getHeight(), img2.getHeight()),
      BufferedImage.TYPE_INT_RGB
    )
    val g = out.createGraphics
    g.drawImage(img1, 0, 0, null)
    g.drawImage(img2, 0, 0, null)
    out
  }

  def main(args: Array[String]): Unit = {
    val inputImagePath = "input.jpg"

    val image: BufferedImage = loadImage(inputImagePath)

    /*for (x <- 0 to (image.getWidth / 4)) {
      for (y <- 0 to (image.getHeight / 4)) {
        image.setRGB(x, y, 0)
      }
    }*/

    val gaussianFilter = new GaussianFilter
    val blurredImage = gaussianFilter.process(image)
    val bwImage = toBW(blurredImage)

    imageToFile(bwImage, "bw.jpg")
    val sobelHorizontal = new CustomFilter(3, 3, Array(1f, 2f, 1f, 0, 0, 0, -1f, -2f, -1f))
    imageToFile(sobelHorizontal.process(bwImage), "s1.jpg")
    val sobelVertical = new CustomFilter(3, 3, Array(1f, 0, -1, 2, 0, -2, 1, 0, -1))
    imageToFile(sobelVertical.process(bwImage), "s2.jpg")

    //val g: Graphics = image.getGraphics

    //out.toByteArray

    /*val outputFilePath = "test.jpg"
    bytesToFile(imageToBytes(blurredImage), outputFilePath)*/
  }

}
