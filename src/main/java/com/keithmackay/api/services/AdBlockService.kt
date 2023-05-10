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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.logging.log4j.util.Strings
import org.bson.Document
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture.supplyAsync

const val pageSize = 2000

@Singleton
class AdBlockService @Inject
internal constructor(
  db: Database,
  ephemeralDatabase: EphemeralDatabase,
  private val grabber: CredentialsGrabber
) {
  private val log = getLogger(this::class)

  private val remoteLsCollection = db.getCollection("lsrules")
  private val localLsCollection = ephemeralDatabase.getCollection("lsrules")
  private val uploadField = "uploadedToNext"

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


  @Throws(IOException::class)
  private fun uploadDeniedServer(server: Server, key: String) {
    val client = OkHttpClient()
    val request: Request = Request.Builder()
      .url("https://api.nextdns.io/profiles/22c2ce/denylist")
      .post(
        RequestBody.create(
          "application/json".toMediaTypeOrNull(),
          Document("id", server.name).append("active", server.active).toJson()
        )
      )
      .header("X-Api-Key", key)
      .build()
    client.newCall(request).execute().use { response ->
      log.info(
        "Response Uploading {}: {}",
        server.name,
        response.body?.string()
      )
    }
    remoteLsCollection.updateOne(Document("server", server.name), set(Document(uploadField, true)), UpdateOptions())
  }

  fun uploadToNextDns() {
    val key: String = grabber.getSecret("next-dns-key").getAsString()
    val server = this.getServerForNextDNS() ?: return
    try {
      uploadDeniedServer(server, key)
    } catch (e: java.lang.Exception) {
      log.warn("Error when uploading server", e)
    }
  }

  private fun getServerForNextDNS(): Server? {
    val list = remoteLsCollection.find(doc("enabled", ne(true)).append(uploadField, ne(true)))
      .limit(1)
      .projection(doc("server", 1))
      .into(ArrayList())
      .map { it.getString("server") }
      .toList()
    return if (list.size == 1) {
      val name = list[0]
      log.info("Adding $name to NextDNS")
      Server(name = name, active = true)
    } else {
      val toRemoveList = remoteLsCollection.find(doc("enabled", ne(false)).append(uploadField, eq(true)))
        .limit(1)
        .projection(doc("server", 1))
        .into(ArrayList())
        .map { it.getString("server") }
        .toList()
      if (list.size == 1) {
        val name = toRemoveList[0]
        log.info("Should remove $name from NextDNS")
        Server(name = name, active = false)
      } else {
        log.info("Nothing to upload to NextDNS")
        null
      }
    }
  }

  fun countNextDnsProgress(): Ratio {
    val uploadedFuture = supplyAsync { remoteLsCollection.countDocuments(doc(uploadField, eq(true))) }
    val totalFuture = supplyAsync { remoteLsCollection.countDocuments() }
    val uploaded = uploadedFuture.get()
    val total = totalFuture.get()
    return Ratio(uploaded, total)
  }

  data class Server(val name: String, val active: Boolean)

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
        localLsCollection.updateOne(
          doc(
            "server",
            eq(it.getString("server"))
          ), doc("\$set", it.drop("_id")),
          UpdateOptions().upsert(true)
        )
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