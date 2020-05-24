package com.cacoveanu.reader.util

import java.util.Optional

object OptionalUtil {
  implicit class AugmentedOptional[T](optional: Optional[T]) {
    def asScala: Option[T] = {
      if (optional.isPresent) {
        Some(optional.get)
      } else {
        None
      }
    }
  }
}
