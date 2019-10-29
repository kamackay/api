package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.Database
import com.keithmackay.api.utils.*
import org.bson.Document


@Singleton
class NewsTask @Inject
internal constructor(db: Database) : Task() {

  private val log = getLogger(this::class)
  private val newsRssCollection = db.getCollection("news_rss")

  override fun time(): Long = 60000

  override fun run() {
    newsRssCollection.find(and(doc("enabled", ne(false))))
        .into(threadSafeList<Document>())
        .parallelStream()
        .forEach {
          val url = it.getString("url")
          val response = khttp.get(url)
          log.info("Request on $url -> ${response.statusCode}")
        }
  }

}