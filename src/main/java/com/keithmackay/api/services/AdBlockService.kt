package com.keithmackay.api.services

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.Database
import com.keithmackay.api.db.EphemeralDatabase
import com.keithmackay.api.utils.*
import com.keithmackay.api.utils.FutureUtils.fastest
import com.mongodb.MongoWriteException
import org.bson.Document
import java.time.Duration
import java.util.concurrent.CompletableFuture.supplyAsync

const val pageSize = 2000

@Singleton
class AdBlockService @Inject
internal constructor(
  private val db: Database,
  private val ephemeralDatabase: EphemeralDatabase
) {
  private val log = getLogger(this::class)

  private val remoteLsCollection = db.getCollection("lsrules")
  private val localLsCollection = ephemeralDatabase.getCollection("lsrules")
  private val rulesCache = Cacher<Set<String>>(Duration.ofMinutes(15), "LS Block Hosts")

  public fun getBlockedServers(): Set<String> {
    return rulesCache.get("all") {
      fastest(supplyAsync {
        remoteLsCollection.find()
          .projection(doc("server", 1))
          .into(ArrayList())
          .map { it.getString("server") }
          .toSet()
      }, supplyAsync {
        localLsCollection.find()
          .projection(doc("server", 1))
          .into(ArrayList())
          .map { it.getString("server") }
          .toSet()
      }).join()
    }
  }

  public fun iterateRemoteServers(): Channel<Document> {
    val channel = Channel<Document>()
    async {
      val count = remoteLsCollection.countDocuments(doc())
      var transferred = 0
      var page = 0
      while (transferred < count) {
        log.info("Transferring Documents in page $page/${count / pageSize}")
        val documents = remoteLsCollection.find(doc())
          .limit(pageSize)
          .skip(page++ * pageSize)
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