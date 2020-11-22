package com.cacoveanu.reader.util

import java.nio.charset.StandardCharsets
import java.util.Base64

import org.springframework.http.{MediaType, ResponseEntity}

object WebUtil {

  def toBase64Image(mediaType: String, image: Array[Byte]) =
    "data:" + mediaType + ";base64," + new String(Base64.getEncoder().encode(image))

  def toResponseEntity(mediaType: String, bytes: Array[Byte]) = mediaType match {
    case FileMediaTypes.TEXT_HTML_VALUE => // todo: probably not happening?
      ResponseEntity.ok().body(new String(bytes, StandardCharsets.UTF_8))

    case FileMediaTypes.TEXT_CSS_VALUE =>
      ResponseEntity.ok().body(new String(bytes, StandardCharsets.UTF_8))

    case FileMediaTypes.TEXT_PLAIN_VALUE =>
      ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(new String(bytes, StandardCharsets.UTF_8))

    case FileMediaTypes.IMAGE_JPEG_VALUE =>
      ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(bytes)

    case FileMediaTypes.IMAGE_PNG_VALUE =>
      ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(bytes)

    case FileMediaTypes.IMAGE_GIF_VALUE =>
      ResponseEntity.ok().contentType(MediaType.IMAGE_GIF).body(bytes)

    case _ => notFound
  }

  def notFound = ResponseEntity.notFound().build()
}
