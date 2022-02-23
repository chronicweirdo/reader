package com.cacoveanu.reader.util

object ManifestUtil {

  val NAME = "chronicreader"
  val DESCRIPTION = "The Chronic Reader web app allows you to access and read your books and comics collection anywhere, on any device."
  val LANG = "en-US"

  def generateManifest(themeColor: String, iconSizes: Array[Int]) = {
    val manifest = new StringBuilder()
    manifest.append("{")
    manifest.append(s""""name": "$NAME",""")
    manifest.append(s""""description": "$DESCRIPTION",""")
    manifest.append(s""""lang": "$LANG",""")

    val iconsString = iconSizes.map(size => s"""{"src": "logo.png?size=$size", "sizes": "${size}x${size}", "type": "image/png"}""").mkString(",")
    manifest.append(s""""icons": [$iconsString],""")

    manifest.append(""""start_url": "/",""")
    manifest.append(""""display": "standalone",""")
    manifest.append(s""""theme_color": "$themeColor"""")

    manifest.append("}")

    manifest.toString()
  }
}
