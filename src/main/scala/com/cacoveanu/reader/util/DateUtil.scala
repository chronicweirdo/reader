package com.cacoveanu.reader.util

import java.text.{ParseException, SimpleDateFormat}
import java.util.Date

object DateUtil {
  private val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")

  def parse(s: String): Option[Date] =
    if (s == null || s.size == 0) None
    else try {
      Some(sdf.parse(s))
    } catch {
      case _: ParseException => None
    }

  def format(d: Date): String = sdf.format(d)
}
