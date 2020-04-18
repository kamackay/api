package com.keithmackay.api.utils

import com.google.inject.Inject
import com.google.inject.Singleton
import org.bson.Document
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

@Singleton
class Credentials @Inject
internal constructor() {

    private val document: Document

    init {
        val str = Files.readString(Paths.get(System.getenv("CREDENTIALS_FILE")),
                StandardCharsets.US_ASCII)
        document = Document.parse(str)
    }

    fun get(s: String) = document[s]

    fun getString(s: String): String = document.getString(s)

    fun getDocument(s: String): Document = document.get(s, Document::class.java)
}
