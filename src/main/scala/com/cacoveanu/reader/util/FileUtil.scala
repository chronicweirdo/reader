package com.cacoveanu.reader.util

import java.io.{File, InputStream}
import java.nio.file.{Files, Paths}
import java.security.{DigestInputStream, MessageDigest}
import java.util.Base64

import org.apache.commons.codec.binary.Base32
import org.springframework.http.MediaType
import org.springframework.web.accept.MediaTypeFileExtensionResolver

import scala.collection.mutable

object FileMediaTypes {

  val IMAGE_JPEG_VALUE = MediaType.IMAGE_JPEG_VALUE
  val IMAGE_PNG_VALUE = MediaType.IMAGE_PNG_VALUE
  val IMAGE_GIF_VALUE = MediaType.IMAGE_GIF_VALUE
  val IMAGE_BMP_VALUE = "image/bmp"
  val TEXT_HTML_VALUE = MediaType.TEXT_HTML_VALUE
  val TEXT_CSS_VALUE = "text/css"

}

object FileTypes {
  val CBR = "cbr"
  val CBZ = "cbz"
  val EPUB = "epub"
}

object FileUtil {

  def getExtension(path: String): String = {
    val lastDotIndex = path.lastIndexOf('.')
    if (lastDotIndex >= 0) path.toLowerCase().substring(lastDotIndex).substring(1)
    else ""
  }

  def isImageType(fileName: String) =
    Seq("jpg", "jpeg", "png", "gif", "bmp") contains getExtension(fileName)

  def getMediaType(fileName: String): Option[String] =
    FileUtil.getExtension(fileName) match {
      case "jpg" => Some(FileMediaTypes.IMAGE_JPEG_VALUE)
      case "jpeg" => Some(FileMediaTypes.IMAGE_JPEG_VALUE)
      case "png" => Some(FileMediaTypes.IMAGE_PNG_VALUE)
      case "gif" => Some(FileMediaTypes.IMAGE_GIF_VALUE)
      case "bmp" => Some(FileMediaTypes.IMAGE_BMP_VALUE)
      case "html" => Some(FileMediaTypes.TEXT_HTML_VALUE)
      case "htm" => Some(FileMediaTypes.TEXT_HTML_VALUE)
      case "xhtml" => Some(FileMediaTypes.TEXT_HTML_VALUE)
      case "css" => Some(FileMediaTypes.TEXT_CSS_VALUE)
      case _ => None
    }

  def getExtensionForMediaType(mediaType: String): Option[String] = mediaType match {
    case FileMediaTypes.IMAGE_BMP_VALUE => Some("bmp")
    case FileMediaTypes.IMAGE_GIF_VALUE => Some("gif")
    case FileMediaTypes.IMAGE_JPEG_VALUE => Some("jpg")
    case FileMediaTypes.IMAGE_PNG_VALUE => Some("png")
    case _ => None
  }

  def getFileName(path: String): String = {
    val pathObject = Paths.get(path);
    val fileName = pathObject.getFileName.toString
    val lastDotIndex = fileName.lastIndexOf('.')
    if (lastDotIndex >= 0) fileName.substring(0, fileName.lastIndexOf('.'))
    else fileName
  }

  def scan(path: String): Seq[File] = {
    var files = mutable.Seq[File]()
    files = files :+ new File(path)
    var processed = 0
    while (processed < files.length) {
      val current = files(processed)
      if (current.exists() && current.isDirectory()) {
        files ++= current.listFiles
      }
      processed += 1
    }

    files.toSeq
  }

  def scanFilesRegex(path: String, regex: String): Seq[String] = {
    val pattern = regex.r
    scan(path)
      .filter(f => f.isFile)
      .filter(f => pattern.pattern.matcher(f.getAbsolutePath).matches)
      .map(f => f.getAbsolutePath)
  }

  def scanFilesRegexWithChecksum(path: String, regex: String): Map[String, String] = {
    val pattern = regex.r
    scan(path)
      .filter(f => f.isFile)
      .filter(f => pattern.pattern.matcher(f.getAbsolutePath).matches)
      .map(f => f.getAbsolutePath)
      .map(f => (getFileChecksum(f), f))
      .toMap
  }

  /*
    SHA1
    SHA-256
    SHA-512
   */
  def getFileChecksum(path: String, algorithm: String = "MD5") = {
    val md = MessageDigest.getInstance(algorithm)
    var is: InputStream = null
    try {
      is = Files.newInputStream(Paths.get(path))
      //val dis = new DigestInputStream(is, md)
      var buffer = new Array[Byte](1024)
      LazyList.continually(is.read(buffer)).takeWhile(_ != -1).foreach(md.update(buffer, 0, _))
    } finally {
      if (is != null) is.close()
    }
    val digest = md.digest
    base32(digest)
  }

  private def base64(arr: Array[Byte]): String = {
    new String(Base64.getEncoder().encode(arr))
  }

  private def base32(arr: Array[Byte]): String = {
    new Base32().encodeAsString(arr).replaceAll("=", "").toLowerCase()
  }
}
