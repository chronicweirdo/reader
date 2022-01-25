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

  def millisToHumanReadable(millis: Long): String = {
    val totalSeconds = millis / 1000
    val remainingMillis = millis % 1000
    val totalMinutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    val totalHours = totalMinutes / 60
    val remainingMinutes = totalMinutes % 60

    val sb = new StringBuilder
    if (totalHours > 0) {
      sb.append(s"${totalHours}h ")
    }
    if (remainingMinutes > 0) {
      sb.append(s"${remainingMinutes}min ")
    }
    if (remainingSeconds > 0) {
      sb.append(s"${remainingSeconds}s ")
    }
    sb.append(s"${remainingMillis}ms")
    sb.toString()
  }

  def main(args: Array[String]): Unit = {
    println(millisToHumanReadable(998))
    println(millisToHumanReadable(12998))
    println(millisToHumanReadable(7*60*1000 + 12998))
    println(millisToHumanReadable(47 * 60 * 60 * 1000 + 7*60*1000 + 12998))
  }
}
