package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.keithmackay.api.db.Database
import com.keithmackay.api.model.Tuple
import com.keithmackay.api.tasks.CronTimes.Companion.minutes
import com.keithmackay.api.utils.*
import com.mongodb.client.MongoCollection
import org.bson.Document
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import twitter4j.Query
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class NewsPriorityTask @Inject internal constructor(db: Database) : CronTask() {
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
    newsCollection // Find all documents that need to have priorities set
        .find(doc("priority", eq(-1)))
        .sort(doc("scrapeTime", 1))
        .limit(10)
        .into(threadSafeList<Document>())
        .stream()
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
  }

  private fun getPriority(doc: Document): Int {
    return try {
      /* Query("url:${}") */
      val query = Query(encode(doc.getString("title")))
      val result = twitter.search(query)
      var retweets: Long = 0
      result.tweets
          .filter { !it.isRetweet }
          .forEach {
            retweets += it.retweetCount + it.favoriteCount + 1
          }
      log.info("This article has received a total of $retweets retweets from ${result.tweets.size} Tweets")
      (retweets / 10).toInt()
    } catch (e: Exception) {
      log.info("Failed to calculate priority from twitter", e)
      -1
    }
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