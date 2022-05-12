package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.Database
import com.keithmackay.api.db.EphemeralDatabase
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
  private val ephemeralDatabase: EphemeralDatabase
) : CronTask() {
  private val log = getLogger(this::class)

  private val pageSize = 2000

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
    val count = remoteCollection.countDocuments(doc())
    var transferred = 0
    var page = 0
    while (transferred < count) {
      log.info("Transferring Documents in page $page")
      val documents = remoteCollection.find(doc())
        .limit(pageSize)
        .skip(page++ * pageSize)
        .into(ArrayList())
      documents.forEach {
        try {
          localCollection.insertOne(it)
        } catch (e: MongoWriteException) {
          // already in the database
          return@forEach
        } catch (e: Exception) {
          log.warn("Couldn't insert", e)
        }
      }
      transferred += documents.size
    }
    log.info("Local: ${localCollection.countDocuments()} - Remote ${remoteCollection.countDocuments()}")
  }

  override fun cron(): String = minutes(10)
}