package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.keithmackay.api.db.EphemeralDatabase
import com.keithmackay.api.email.EmailSender
import com.keithmackay.api.model.Tuple
import com.keithmackay.api.tasks.CronTimes.Companion.seconds
import com.keithmackay.api.utils.*
import com.mongodb.client.MongoCollection
import org.bson.Document
import org.json.JSONArray
import org.json.JSONObject
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import twitter4j.Query
import twitter4j.Status
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.util.concurrent.CompletableFuture
import kotlin.math.ceil

const val HOUR = 1000 * 60 * 60
const val ARTICLES_AT_ONCE = 2

class NewsPriorityTask @Inject internal constructor(
  db: EphemeralDatabase,
  private val emailSender: EmailSender
) : CronTask() {
  private val log = getLogger(this::class)

  private val newsCollection: MongoCollection<Document> = db.getCollection("news")
  private val twitter: Twitter = TwitterFactory(
    ConfigurationBuilder()
      .setGZIPEnabled(true)
      .setTweetModeExtended(true)
      .build()
  )
    .instance

  @Throws(JobExecutionException::class)
  override fun execute(jobExecutionContext: JobExecutionContext) {
    val start = System.currentTimeMillis()
    val l = mutableListOf<Document>()
    this.getArticle().forEach { it?.let(l::add) }
    l.parallelStream()
      .map { doc: Document -> Tuple(doc, getPriority(doc)) }
      .forEach { tuple: Tuple<Document, Int> ->
        try {
          val doc = tuple.getA()
          val currentPriority = doc.getInteger("priority")
          val priority = tuple.getB().coerceAtLeast(currentPriority)
          log.debug(
            newsCollection.updateOne(
              doc("_id", eq(doc.getObjectId("_id"))),
              set(
                doc("priority", priority)
                  .add("priorityUpdated", System::currentTimeMillis)
              ).append("\$inc", doc("timesPrioritized", 1))
            )
          )
          log.info("${doc.getString("title")} -> New Priority $priority (was $currentPriority)")
          if (!this.shouldNotify(currentPriority) && this.shouldNotify(tuple.getB())) {
            // Send email that new Important News Item has been found
            log.info("Sending Important Article Email")
            val source = doc.subDoc("source").getString("site")
            val title = doc.getString("title")
            /*emailSender.send("New Article from $source: $title",
              doc.getString("description"),
              emailSender.mainUser())*/
          }
        } catch (e: Exception) {
          log.error("Error Updating Priority", e)
        }
      }

    val prioritized: Double = newsCollection.find(doc("priority", gte(0))).count().toDouble()
    val total: Double = newsCollection.countDocuments().toDouble()

    log.info(
      "Finished News Priority Task (${printTimeDiff(start)}) ${prioritized.toInt()}/${total.toInt()} prioritized: ${
        DecimalFormat("##.#%").format(prioritized / total)
      }"
    )
  }

  private fun shouldNotify(priority: Int) = false //priority > 2500

  private fun getArticle(): List<Document?> {
    val articles = newsCollection
      .find(
        // Don't update a document that was posted during the current hour
        doc("time", lte(System.currentTimeMillis() - HOUR))
          .append("priorityUpdated", lte(System.currentTimeMillis() - HOUR))
          .append("priority", -1)
      )
      .sort(
        doc("priorityUpdated", 1)
          .append("timesPrioritized", 1)
          .append("time", -1)
      )
      .limit(ARTICLES_AT_ONCE)
      .into(ArrayList())

    // If tweet was last updated within a minute
    if (articles.isEmpty()) {
      log.warn("Couldn't find any articles to prioritize, grabbing a random one")
      return this.getRandomArticle()
    }
    return articles
  }

  private fun getRandomArticle(count: Int = ARTICLES_AT_ONCE) = newsCollection
    .find(
        doc("priorityUpdated", lte(System.currentTimeMillis() - HOUR))
    )
    .sort(
      doc("timesPrioritized", 1)
        .append("priorityUpdated", 1)
    )
    .limit(count)
    .into(ArrayList())

  private fun getPriority(doc: Document): Int {
    //val twitterFuture = CompletableFuture.supplyAsync { getPriorityFromTwitter(doc) }
    val redditFuture = CompletableFuture.supplyAsync { getPriorityFromReddit(doc) }
    return redditFuture.get().coerceAtLeast(0)
  }

  private fun getPriorityFromReddit(doc: Document): Int {
    try {
      val response = httpGet("https://www.reddit.com/api/info.json?url=${doc.getString("link")}")
      val json = JSONObject(response.body!!.string())
      if ("Listing" == json.getString("kind")) {
        val data = json.getJSONObject("data")
        val children = if (data.has("children")) {
          data.getJSONArray("children")
        } else {
          JSONArray()
        }
        var score = 0
        for (x in 0 until children.length()) {
          score += getScoreFromRedditPost(children.getJSONObject(x))
        }
        log.info("This article has received a total of $score interactions from ${children.length()} Reddit Posts")
        return score
      } else {
        log.warn("Kind of response was ${json.getString("kind")}")
      }
    } catch (e: Exception) {
      log.error("Failed to calculate priority from Reddit", e)
    }
    return -1
  }

  private fun getScoreFromRedditPost(post: JSONObject): Int {
    return try {
      val ups = post.getJSONObject("data").getInt("ups")
      var upsFromParents = 0
      val parents = if (post.has("crosspost_parent_list")) {
        post.getJSONArray("crosspost_parent_list")
      } else {
        JSONArray()
      }
      for (x in 0 until parents.length()) {
        upsFromParents += parents.getJSONObject(x).getInt("ups")
      }
      return ups + upsFromParents + 1
    } catch (e: Exception) {
      log.error("Error getting score from post", e)
      0
    }
  }

  private fun getPriorityFromTwitter(doc: Document): Int {
    return try {
      val tweets = this.search(
        "\"${doc.getString("title")}\"",
        "url:${encode(doc.getString("link"))}"
      )
      var interactions = 0
      tweets
        .forEach {
          it.retweetedStatus
          interactions += it.interactions()
        }
      log.info(
        "This article has received a total of $interactions interactions from ${tweets.size} Tweets " +
            "([${doc.subDoc("source").getString("site")}] ${doc.getString("title")})"
      )
      ceil(interactions.toDouble() / tweets.size).toInt()
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
          log.warn("Could not search Twitter, ${e.message}", e)
          listOf<Status>()
        }
      }
      .map {
        it.map(Status::exposeParent)
          .map(Array<Status>::toList)
          .flatten()
          .distinctBy(Status::getId)
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


  override fun cron(): String = seconds(10)

  override fun name(): String = "NewsPriorityTask"

  data class NewsItemEmail(
    val title: String,
    val source: String,
    val url: String,
    val content: String
  )

}

fun Status.exposeParent(): Array<Status> {
  // If this is a retweet, and that original tweet is more popular, get it
  val rs = this.retweetedStatus
  if (this.isRetweet && this.retweetedStatus != null && rs.interactions() > this.interactions()) {
    return arrayOf(this, *rs.exposeParent())
  }
  return arrayOf(this)
}

fun Status.interactions(): Int = this.retweetCount + this.favoriteCount + 2
