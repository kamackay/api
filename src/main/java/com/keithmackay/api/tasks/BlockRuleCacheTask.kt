package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.Database
import com.keithmackay.api.db.EphemeralDatabase
import com.keithmackay.api.services.AdBlockService
import com.keithmackay.api.tasks.CronTimes.Companion.minutes
import com.keithmackay.api.utils.doc
import com.keithmackay.api.utils.getLogger
import com.keithmackay.api.utils.stepCarefully
import com.mongodb.MongoWriteException
import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.client.model.IndexOptions
import org.quartz.JobExecutionContext

@Singleton
class BlockRuleCacheTask
@Inject internal constructor(
  private val db: Database,
  private val ephemeralDatabase: EphemeralDatabase,
  private val adBlockService: AdBlockService
) : CronTask() {
  private val log = getLogger(this::class)

  private val remoteCollection = db.getCollection("lsrules")
  private val localCollection = ephemeralDatabase.getOrMakeCollection("lsrules", CreateCollectionOptions())

  override fun name(): String = "BlockRuleCacheTask"

  override fun execute(context: JobExecutionContext?) {
    stepCarefully(listOf {
      localCollection.createIndex(
        doc("server", 1),
        IndexOptions().unique(true)
      )
      Unit
    }) {
      log.info("Index Already Exists")
    }
    adBlockService.iterateRemoteServers().iterate {
      try {
        localCollection.insertOne(it)
      } catch (e: MongoWriteException) {
        // already in the database
        return@iterate
      } catch (e: Exception) {
        log.warn("Couldn't insert", e)
      }
    }
    val local = localCollection.countDocuments()
    val remote = remoteCollection.countDocuments()
    log.info("Local: $local - Remote $remote${if (remote == local) "" else "!!!!!"}")
  }

  override fun cron(): String = minutes(10)
}