package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.EphemeralDatabase
import com.keithmackay.api.utils.*
import org.quartz.JobExecutionContext
import java.util.stream.Collectors.toList

@Singleton
class NewsConversationCleanupTask @Inject
internal constructor(
  private val ephemeralDatabase: EphemeralDatabase
) : CronTask() {
  private val log = getLogger(this::class)
  private val newsCollection = ephemeralDatabase.getCollection("news")
  private val conversationCollection = ephemeralDatabase.getCollection("news_conversation")

  override fun execute(jobExecutionContext: JobExecutionContext) {
    try {
      val existingGuids = newsCollection.distinct("guid", String::class.java)
              .into(HashSet())
      val result = conversationCollection.deleteMany(
              or(
                      and(*existingGuids.stream()
                              .map { doc("article", ne(it)) }
                              .collect(toList())
                              .toTypedArray()
                      ),
                      doc("score", eq(0)))) // Or the Score is 0
      log.info("Deleted ${result.deletedCount} Links from Conversation Database")
    } catch (e: Exception) {
      log.error("Error in Task", e)
    }
  }

  override fun name(): String = "NewsConversationCleanupTask"

  override fun cron(): String = CronTimes.minutes(10)
}