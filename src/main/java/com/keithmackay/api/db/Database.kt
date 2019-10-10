package com.keithmackay.api.db

import com.google.gson.JsonParser
import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.utils.fileToString
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoCollection
import org.bson.Document

@Singleton
class Database @Inject
internal constructor() {

  private val client: MongoClient

  init {
    this.client = MongoClient(MongoClientURI(
        "mongodb+srv://admin:${getPassword()}@apicluster-tsly9.mongodb.net/test?retryWrites=true&w=majority"))
  }

  private fun getPassword(): String =
      JsonParser().parse(fileToString(System.getenv("CREDENTIALS_FILE")).trim())
          .asJsonObject
          .get("password")
          .asString

  fun getCollection(db: String, name: String): MongoCollection<Document> =
      this.client.getDatabase(db).getCollection(name)

  fun getCollection(name: String): MongoCollection<Document> =
      getCollection("api", name)
}