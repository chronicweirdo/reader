package com.cacoveanu.reader.service

import java.awt.Graphics
import java.awt.image.{BufferedImage, BufferedImageOp, ConvolveOp, Kernel}
import java.io.{BufferedOutputStream, ByteArrayInputStream, ByteArrayOutputStream, FileOutputStream}
import java.nio.file.{Files, Paths}

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

  def main(args: Array[String]): Unit = {
    val inputImagePath = "C:\\Users\\silvi\\Desktop\\Howard the Duck 006-005 (AnPymGold-Empire).jpg"

    val image: BufferedImage = loadImage(inputImagePath)

    /*for (x <- 0 to (image.getWidth / 4)) {
      for (y <- 0 to (image.getHeight / 4)) {
        image.setRGB(x, y, 0)
      }
    }*/

    val blurFilter = new BlurFilter
    val blurredImage = blurFilter.process(image)


    //val g: Graphics = image.getGraphics

    //out.toByteArray

    val outputFilePath = "test.jpg"
    bytesToFile(imageToBytes(blurredImage), outputFilePath)
  }

}
