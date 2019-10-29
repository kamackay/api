package com.keithmackay.api.db

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.utils.SecretGrabber
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.CreateCollectionOptions
import org.bson.Document
import org.jongo.Jongo

@Singleton
class Database @Inject
internal constructor(secretGrabber: SecretGrabber) : IDatabase {

  private val client: MongoClient

  private val connectionString: String

  init {
    val pass = secretGrabber.getSecret("mongo-password").asString
    this.connectionString = "mongodb+srv://admin:$pass@apicluster-tsly9.mongodb.net/test?retryWrites=true&w=majority"
    this.client = MongoClient(MongoClientURI(this.connectionString))
  }

  override fun getCollection(db: String, name: String): MongoCollection<Document> =
      this.client.getDatabase(db).getCollection(name)

  override fun getCollection(name: String): MongoCollection<Document> =
      getCollection("api", name)

  override fun getJongoCollection(name: String?): org.jongo.MongoCollection =
      Jongo(this.client.getDB("api")).getCollection("name")

  fun getOrMakeCollection(db: String, name: String, opts: CreateCollectionOptions?): MongoCollection<Document> {
    val dbInstance = client.getDatabase(db)
    if (!dbInstance.listCollectionNames().contains(name)) {
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
