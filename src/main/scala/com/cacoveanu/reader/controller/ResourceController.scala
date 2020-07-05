package com.cacoveanu.reader.controller

/**
 * Load resource from book by path or ID, return that resource (after processing) in the correct format.
 * May want to request image in Base64 format as well.
 */
class ResourceController {

  def loadResourceByPath(bookId: String, resourcePath: String) = None

  def loadResourceByIndex(bookId: String, index: Int) = None

  def loadImageByIndexBase64(bookId: String, index: Int) = None
}
