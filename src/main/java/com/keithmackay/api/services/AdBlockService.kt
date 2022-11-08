package com.keithmackay.api.services

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.Database
import com.keithmackay.api.db.EphemeralDatabase
import com.keithmackay.api.utils.*
import com.keithmackay.api.utils.FutureUtils.fastest
import com.mongodb.MongoWriteException
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.UpdateOptions
import org.apache.logging.log4j.util.Strings
import org.bson.Document
import java.util.*
import java.util.concurrent.CompletableFuture.supplyAsync

const val pageSize = 2000

@Singleton
class AdBlockService @Inject
internal constructor(
    db: Database,
    ephemeralDatabase: EphemeralDatabase
) {
  private val log = getLogger(this::class)

  private val remoteLsCollection = db.getCollection("lsrules")
  private val localLsCollection = ephemeralDatabase.getCollection("lsrules")

  fun getBlockedServers(): Set<String> {
    return fastest(supplyAsync {
      remoteLsCollection.find(doc("enabled", ne(false)))
          .projection(doc("server", 1))
          .into(ArrayList())
          .map { it.getString("server") }
          .toSet()
    }, supplyAsync {
      localLsCollection.find(doc("enabled", ne(false)))
          .projection(doc("server", 1))
          .into(ArrayList())
          .map { it.getString("server") }
          .toSet()
    }).join()
  }

  fun getRecords(): Collection<Document> {
    return localLsCollection.find()
        .projection(doc("server", 1).append("source", 1))
        .into(ArrayList())
  }

  fun isBlocked(domain: String): Boolean {
    val lowerDomain = domain.lowercase()
    return getBlockedServers()
        .stream()
        .filter(Objects::nonNull)
        .filter(Strings::isNotEmpty)
        .map(String::lowercase)
        .anyMatch(lowerDomain::equals)
  }

  fun doCacheSync(trigger: String = "manual", chunkSize: Int = pageSize): CacheSyncResult {
    val localDocs = localLsCollection.countDocuments()
    val remoteDocs = remoteLsCollection.countDocuments()
    if (localDocs == remoteDocs) {
      log.info("Number of documents in collections are already the same")
      return CacheSyncResult(localDocs, remoteDocs)
    }
    log.info("Running cache sync. Trigger: $trigger")
    stepCarefully(listOf {
      localLsCollection.createIndex(
          doc("server", 1),
          IndexOptions().unique(true)
      )
      Unit
    }) {
      log.info("Index Already Exists")
    }
    iterateRemoteServers(chunkSize).iterate {
      try {
        localLsCollection.updateOne(doc("server",
            eq(it.getString("server"))), doc("\$set", it.drop("_id")),
            UpdateOptions().upsert(true))
      } catch (e: MongoWriteException) {
        // already in the database
        return@iterate
      } catch (e: Exception) {
        log.error("Couldn't insert", e)
      }
    }
    val local = localLsCollection.countDocuments()
    val remote = remoteLsCollection.countDocuments()
    log.info("Local: $local - Remote $remote${if (remote == local) "" else "!!!!!"}")
    return CacheSyncResult(local, remote)
  }

  data class CacheSyncResult(val local: Long, val remote: Long)

  private fun iterateRemoteServers(chunkSize: Int = pageSize): Channel<Document> {
    val channel = Channel<Document>()
    async {
      val count = remoteLsCollection.countDocuments(doc())
      var transferred = 0
      var page = 0
      while (transferred < count) {
        log.info("Transferring Documents in page $page/${count / chunkSize}")
        val documents = remoteLsCollection.find(doc())
            .limit(chunkSize)
            .skip(page++ * chunkSize)
            .into(ArrayList())
        documents.forEach {
          log.debug("Pushing document to channel")
          channel.push(it)
        }
        transferred += documents.size
      }
      channel.close()
    }
    return channel
  }
}