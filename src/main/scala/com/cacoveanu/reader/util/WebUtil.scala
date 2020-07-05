package com.cacoveanu.reader.util

import java.util.Base64

object WebUtil {

  def toBase64Image(mediaType: String, image: Array[Byte]) =
    "data:" + mediaType + ";base64," + new String(Base64.getEncoder().encode(image))
}
