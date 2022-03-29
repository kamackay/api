package com.keithmackay.api.services

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.Database
import com.keithmackay.api.db.EphemeralDatabase
import com.keithmackay.api.utils.*
import com.mongodb.client.FindIterable
import org.bson.Document

@Singleton
class NewsService @Inject
internal constructor(
  db: Database,
  private val ephemeralDatabase: EphemeralDatabase
) {
  private val log = getLogger(this::class)

  private val newsRssCollection = db.getCollection("news_rss")

  val defaultNewsSort: Document = doc("priority", -1)
    .append("time", -1)

  fun getDaysTopNews(limit: Int = 20): List<NewsItem> {
    val l = mutableListOf<NewsItem>()
    try {
      l.addAll(
        ephemeralDatabase.getCollection("news")
          .find()
          .sort(defaultNewsSort)
          .limit(limit)
          .map(this::docToNews)
          .into(ArrayList<NewsItem>())
      )
    } catch (e: Exception) {
      log.error("Error Getting News", e)
    }
    return l
  }

  fun getSources(): List<Document> = newsRssCollection.find(and(doc("enabled", ne(false))))
    .into(threadSafeList<Document>())

  fun getAll(): FindIterable<Document> = ephemeralDatabase.getCollection("news").find()
    .sort(defaultNewsSort)
    .limit(1000)

  private fun docToNews(doc: Document): NewsItem {
    val source = doc.subDoc("source")
    return NewsItem(
      time = doc.getLong("time"),
      title = doc.getString("title"),
      link = doc.getString("link"),
      source = NewsSource(
        group = source.getString("group"),
        site = source.getString("site"),
        url = source.getString("url")
      ),
      index = doc.getInteger("indexInFeed"),
      content = doc.getString("content"),
      pubDate = doc.getString("pubDate")
    )
  }


  data class NewsItem(
    val time: Long,
    val index: Int,
    val title: String,
    val link: String,
    val source: NewsSource,
    val content: String?,
    val pubDate: String?
  )

  data class NewsSource(
    val group: String,
    val site: String,
    val url: String
  )
}