package com.cacoveanu.reader.service

import scala.collection.immutable

object BatchTest {

  private def toBatches[T](data: Seq[T], batchSize: Int = 10): Seq[Seq[T]] = {
    data.zipWithIndex
      .groupBy { case (entry, index) => index / batchSize}
      .map { case (groupIndex, groupList) => groupList.map {
        case (entry, index) => entry
      }
      }.toSeq
  }

  def main(args: Array[String]): Unit = {
    val data = 1 to 100
    /*val batches: immutable.Iterable[IndexedSeq[Int]] = data
      .zipWithIndex
      .groupBy { case (e, i) => i / 10}
      .map { case (i, list) => list.map { case (e, _) => e} }*/
    //println(batches)
    println(toBatches(data, 20))
  }
}
