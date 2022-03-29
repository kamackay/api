package com.keithmackay.api.db

import com.google.inject.Inject
import com.keithmackay.api.utils.getLogger
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.CreateCollectionOptions
import org.bson.Document
import javax.inject.Singleton

/**
 * Database Internal to the Cluster to reduce Latency
 */
@Singleton
class EphemeralDatabase @Inject
internal constructor() : IDatabase {
  private val log = getLogger(this::class)

  private val client: MongoClient

  init {
    log.info("Connecting to (Ephemeral) mongo with Default options")
    this.client = MongoClients.create("mongodb://db:27017")
  }

  override fun getCollection(db: String, name: String): MongoCollection<Document> =
    this.client.getDatabase(db).getCollection(name)

  override fun getCollection(name: String): MongoCollection<Document> =
    getCollection("api", name)

  override fun getJongoCollection(name: String?): org.jongo.MongoCollection =
    throw NotImplementedError("Not Using Jongo")

  private fun getOrMakeCollection(db: String, name: String, opts: CreateCollectionOptions?): MongoCollection<Document> {
    val dbInstance = client.getDatabase(db)
    if (!dbInstance.listCollectionNames().contains(name)) {
      this.log.info("Collection $name did not exist, creating")
      if (opts == null) {
        dbInstance.createCollection(name)
      } else {
        dbInstance.createCollection(name, opts)
      }
    }
    return this.getCollection(db, name)
  }

  fun getOrMakeCollection(name: String, opts: CreateCollectionOptions): MongoCollection<Document> =
    this.getOrMakeCollection("api", name, opts)

  override fun getConnectionString(): String = ""
}