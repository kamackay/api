package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.Database
import com.keithmackay.api.utils.*
import org.bson.Document
import javax.xml.parsers.DocumentBuilderFactory


@Singleton
class NewsTask @Inject
internal constructor(db: Database) : Task() {

  private val log = getLogger(this::class)
  private val newsRssCollection = db.getCollection("news_rss")

  override fun time(): Long = 60000

  override fun run() {
    newsRssCollection.find(and(doc("enabled", ne(false))))
        .into(threadSafeList<Document>())
        .forEach {
          val url = it.getString("url")
          try {
            val response = khttp.get(url)
            val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val document = docBuilder.parse(inputStream(response.text))
            val channels = document.getElementsByTagName("channel")
            log.info("${channels.length} Channels on $url", channels.item(0).toString())
          } catch (e: Exception) {
            log.error("Error on $url", e)
          }
        }
  }

}