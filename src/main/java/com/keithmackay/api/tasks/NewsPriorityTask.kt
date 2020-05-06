package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.keithmackay.api.db.EphemeralDatabase
import com.keithmackay.api.model.Tuple
import com.keithmackay.api.tasks.CronTimes.Companion.minutes
import com.keithmackay.api.utils.*
import com.mongodb.client.MongoCollection
import org.bson.Document
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import twitter4j.Query
import twitter4j.Status
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class NewsPriorityTask @Inject internal constructor(db: EphemeralDatabase) : CronTask() {
  private val log = getLogger(this::class)
  private val newsCollection: MongoCollection<Document> = db.getCollection("news")
  private val twitter: Twitter = TwitterFactory(ConfigurationBuilder()
      .setGZIPEnabled(true)
      .setTweetModeExtended(true)
      .build())
      .instance

  @Throws(JobExecutionException::class)
  override fun execute(jobExecutionContext: JobExecutionContext) {
    log.info("Starting News Priority Task")
    val start = System.currentTimeMillis()
    val l = newsCollection // Find all documents that need to have priorities set
        .find(doc("priority", -1))
        .sort(doc("time", 1))
        .limit(100)
        .into(threadSafeList<Document>())
    log.info("Found ${l.size} articles that need to be prioritized")
    l.stream()
        .map { doc: Document -> Tuple(doc, getPriority(doc)) }
        .forEach { tuple: Tuple<Document, Int> ->
          if (tuple.getB() == -1) {
            return@forEach
          }
          try {
            newsCollection.updateOne(
                doc("_id", eq(tuple.getA().getObjectId("_id"))),
                set(doc("priority", tuple.getB())))
          } catch (e: Exception) {
            log.error("Error Updating Priority", e)
          }
        }
    log.info("Finished News Priority Task (${millisToReadableTime(System.currentTimeMillis() - start)})")
  }

  private fun getPriority(doc: Document): Int {
    return try {
      val tweets = this.search("\"${doc.getString(" title ")}\"",
          "url:${encode(doc.getString("link"))}")
      var interactions: Long = 0
      tweets
          .forEach {
            interactions += it.retweetCount + it.favoriteCount + 1
          }
      log.info("This article has received a total of $interactions retweets from ${tweets.size} Tweets " +
          "(${doc.getString("title")})")
      (interactions / 10).toInt() - 1 // 0 will evaluate as -1
    } catch (e: Exception) {
      log.info("Failed to calculate priority from twitter", e)
      -1
    }
  }

  private fun search(vararg query: String): List<Status> {
    val list = mutableListOf<Status>()
    query
        .map { Query(it) }
        .map {
          try {
            twitter.search(it).tweets
          } catch (e: Exception) {
            log.warn("Could not search Twitter")
            listOf<Status>()
          }
        }
        .forEach { list.addAll(it) }
    return list
  }

  private fun encode(s: String): String =
      try {
        URLEncoder.encode(s, StandardCharsets.UTF_8.toString())
      } catch (e: Exception) {
        s
      }


  override fun cron(): String = minutes(5)

  override fun name(): String = "NewsPriorityTask"

}