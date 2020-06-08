package com.cacoveanu.reader.util

import java.util.Optional

object SeqUtil {
  implicit class AugmentedSeq[T](data: Seq[T]) {
    def toBatches(batchSize: Int = 10): Seq[Seq[T]] = {
      data.zipWithIndex
        .groupBy { case (entry, index) => index / batchSize}
        .map { case (groupIndex, groupList) => groupList.map {
          case (entry, index) => entry
        }
        }.toSeq
    }
  }
}
