package com.keithmackay.api.db

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.minutes
import com.keithmackay.api.utils.SecretGrabber
import com.keithmackay.api.utils.getLogger
import com.keithmackay.api.utils.threadSafeMap
import com.mongodb.MongoClient
import com.mongodb.MongoClientOptions
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.CreateCollectionOptions
import org.bson.Document
import org.jongo.Jongo

@Singleton
class Database @Inject
internal constructor(secretGrabber: SecretGrabber) : IDatabase {

  private val log = getLogger(this::class)

  private val client: MongoClient

  private val connectionString: String

  init {
    val pass = secretGrabber.getSecret("mongo-password").asString
    val optionMap = threadSafeMap<String, Any>()
    optionMap["retryWrites"] = true
    optionMap["w"] = "majority"
    optionMap["maxPoolSize"] = 8
    optionMap["minPoolSize"] = 2
    optionMap["socketTimeoutMS"] = 60 * 1000
    val options = optionMap.map { "${it.key}=${it.value}" }
        .joinToString(separator = "&")
    log.info("Connecting to mongo with URL Options: $options")
    this.connectionString = "mongodb+srv://admin:$pass@apicluster-tsly9.mongodb.net/?$options"
    this.client = MongoClient(MongoClientURI(this.connectionString,
        MongoClientOptions.builder()
            .maxConnectionIdleTime(0)
            .maxWaitTime(minutes(1).toInt())))
  }

  override fun getCollection(db: String, name: String): MongoCollection<Document> =
      this.client.getDatabase(db).getCollection(name)

  override fun getCollection(name: String): MongoCollection<Document> =
      getCollection("api", name)

  override fun getJongoCollection(name: String?): org.jongo.MongoCollection =
      Jongo(this.client.getDB("api")).getCollection("name")

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

  override fun getConnectionString(): String = this.connectionString
}
