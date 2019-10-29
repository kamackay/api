package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.MINUTE
import com.keithmackay.api.db.Database
import com.keithmackay.api.utils.*
import com.mongodb.MongoWriteException
import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.client.model.IndexOptions
import org.bson.Document
import org.w3c.dom.Node
import java.io.StringWriter
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


@Singleton
class NewsTask @Inject
internal constructor(private val db: Database) : Task() {

  private val log = getLogger(this::class)
  private val newsRssCollection = db.getCollection("news_rss")

  override fun time(): Long = MINUTE * 2

  override fun run() {
    // This allows dropping the collection to clear old news
    val newsCollection = db.getOrMakeCollection("news",
        CreateCollectionOptions()
            .sizeInBytes(1000 * 1000 * 10) // 10 MB
            .maxDocuments(1000)
            .capped(true))
    val indexName = newsCollection.createIndex(doc("guid", -1),
        IndexOptions()
            .name("guid-unique")
            .unique(true))
    log.info("Verified Index $indexName")
    val existingGuids = newsCollection.distinct("guid", String::class.java)
    newsRssCollection.find(and(doc("enabled", ne(false))))
        .into(threadSafeList<Document>())
        .forEach { dbDoc ->
          val url = dbDoc.getString("url")
          try {
            val response = khttp.get(url)
            val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val document = docBuilder.parse(inputStream(response.text))
            val channels = document.getElementsByTagName("channel")
            log.debug("${channels.length} Channels on $url")
            IntRange(0, channels.length - 1)
                .map(channels::item)
                .forEach { node ->
                  log.debug(node.toXml())
                  val items = node.getChildrenByTag("item")
                  log.debug("$url has ${items.size} items")
                  items.forEachIndexed { x, item ->
                    val newsItem = doc("source", cleanDoc(dbDoc))
                        .add("time", System.currentTimeMillis())
                        .add("importance", x.toLong())
                    val title = item.addPropToDocument("title", newsItem)
                    item.addPropToDocument("link", newsItem)
                    item.addPropToDocument("dc:creator", newsItem)
                    item.getFirstChildByTag("content:encoded")
                        .map { it.textContent }
                        .map(::purgeHtml)
                        .ifPresent { value ->
                          newsItem.append("content", value)
                        }
                    item.addPropToDocument("description", newsItem) {
                      log.info("Could not find description!")
                    }
                    item.addPropToDocument("pubDate", newsItem)
                    newsItem.append("categories", item.getChildrenByTag("category")
                        .map { it.textContent })
                    val guid = item.addPropToDocument("guid", newsItem) {
                      log.warn("Could Not Find GUID on item!")
                    }
                    if (guid != null && !existingGuids.contains(guid)) {
                      try {
                        newsCollection.insertOne(newsItem)
                        log.info("Successfully Added News from ${dbDoc.getString("site")}: $title")
                      } catch (me: MongoWriteException) {
                        log.debug("Could not update document due to Static Size Limit: $guid")
                      }
                    }
                  }
                }
          } catch (e: Exception) {
            log.error("Error on $url", e)
          }
        }
  }

  private fun Node.addPropToDocument(name: String, doc: Document, notPresent: () -> Unit = {}): String? {
    var s: String? = null
    this.getFirstChildByTag(name)
        .map { it.textContent }
        .ifPresentOrElse({ value ->
          doc[name] = value
          s = value
        }, notPresent)
    return s
  }

  private fun Node.getChildrenByTag(tag: String): List<Node> =
      IntRange(0, this.childNodes.length - 1)
          .map(this.childNodes::item)
          .filter { it.nodeName == tag }

  private fun Node.getFirstChildByTag(tag: String): Optional<Node> =
      Optional.ofNullable(IntRange(0, this.childNodes.length - 1)
          .map(this.childNodes::item)
          .firstOrNull { it.nodeName == tag })

  private fun Node.toXml(): String {
    val sw = StringWriter()
    try {
      val t = TransformerFactory.newInstance().newTransformer()
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
      t.setOutputProperty(OutputKeys.INDENT, "yes")
      t.transform(DOMSource(this@toXml), StreamResult(sw))
    } catch (te: TransformerException) {
      log.error("nodeToString Transformer Exception", te)
    }

    return sw.toString()
  }

}