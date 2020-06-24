package com.cacoveanu.reader.service
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Base64
import org.apache.commons.codec.binary.Base32

object ChecksumTest {

  /*
    SHA1
    SHA-256
    SHA-512
   */
  def getFileChecksum(path: String, algorithm: String = "MD5") = {
    val md = MessageDigest.getInstance(algorithm)
    try {
      val is = Files.newInputStream(Paths.get(path))
      val dis = new DigestInputStream(is, md)
      var buffer = new Array[Byte](1024)
      LazyList.continually(is.read(buffer)).takeWhile(_ != -1).foreach(md.update(buffer, 0, _))
    }
    val digest = md.digest
    digest
  }

  def base64(arr: Array[Byte]): String = {
    new String(Base64.getEncoder().encode(arr))
  }

  def base64Url(arr: Array[Byte]): String = {
    new String(Base64.getUrlEncoder().encode(arr))
  }

  def base32(arr: Array[Byte]): String = {
    new Base32().encodeAsString(arr).replaceAll("=", "").toLowerCase()
  }

  /*import java.io.FileInputStream
  import java.io.InputStream
  import java.security.MessageDigest

  def checksum(path: String): Array[Byte] = {
    try {
      val in = Files.newInputStream(Paths.get(path))
      try {
        val digest = MessageDigest.getInstance("MD5")
        val block = new Array[Byte](4096)
        var length = 0
        while ( {
          (length = in.read(block)) > 0
        }) digest.update(block, 0, length)
        return digest.digest
      } catch {
        case e: Exception =>
          e.printStackTrace()
      } finally if (in != null) in.close()
    }
    null
  }*/

  def main(args: Array[String]): Unit = {
    val f1 = "C:\\Users\\silvi\\Desktop\\comic1.cbz"
    val f2 = "C:\\Users\\silvi\\Desktop\\comic2.cbz"
    val f3 = "C:\\Users\\silvi\\Desktop\\a very different name for a comic.cbz"

    println(base64(getFileChecksum(f1)))
    println(base64(getFileChecksum(f2)))
    println(base64(getFileChecksum(f3)))
    println(base64Url(getFileChecksum(f1)))
    println(base64Url(getFileChecksum(f2)))
    println(base64Url(getFileChecksum(f3)))
    println(base32(getFileChecksum(f1)))
    println(base32(getFileChecksum(f2)))
    println(base32(getFileChecksum(f3)))

  }

}
