package com.keithmackay.api.db

import org.bson.Document

interface JsonObj<T> {
  fun toJson(): String
  fun fromJson(doc: Document): T
}
