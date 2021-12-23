package com.keithmackay.api.utils

import com.google.inject.Inject
import com.google.inject.Singleton
import org.bson.Document
import java.io.File
import java.nio.file.Paths

@Singleton
class Credentials @Inject
internal constructor() {

  private val document: Document

  init {
    val str = File(Paths.get(System.getenv("CREDENTIALS_FILE")).toAbsolutePath().toString())
      .readText(Charsets.UTF_8)
    document = Document.parse(str)
  }

  fun get(s: String) = document[s]

  fun getString(s: String): String = document.getString(s)

  fun getDocument(s: String): Document = document.get(s, Document::class.java)
}
