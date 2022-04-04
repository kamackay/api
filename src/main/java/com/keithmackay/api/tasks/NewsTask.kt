package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.Database
import com.keithmackay.api.db.EphemeralDatabase
import com.keithmackay.api.megabytes
import com.keithmackay.api.minutes
import com.keithmackay.api.utils.*
import com.mongodb.MongoWriteException
import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.client.model.IndexOptions
import okhttp3.ResponseBody
import okhttp3.internal.readBomAsCharset
import org.bson.Document
import org.w3c.dom.Node
import org.xml.sax.SAXParseException
import java.io.StringWriter
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executors.newFixedThreadPool
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

val Encoding: Charset = UTF_8
val Encoder: Base64.Encoder = Base64.getEncoder()

@Singleton
class NewsTask @Inject
internal constructor(
  db: Database,
  private val ephemeralDatabase: EphemeralDatabase
) : Task() {

  private val log = getLogger(this::class)
  private val newsRssCollection = db.getCollection("news_rss")
  private val excludedServers = getExcludedServers(db)
  private val threadPool = newFixedThreadPool(1)

  override fun time(): Long = minutes(2)

  override fun run() {
    // This allows dropping the collection to clear old news
    val newsCollection = ephemeralDatabase.getOrMakeCollection(
      "news",
      CreateCollectionOptions()
        .capped(true)
        .sizeInBytes(megabytes(2.5))
        .maxDocuments(2500)
    )
    stepCarefully(
      listOf {
        newsCollection.createIndex(
          doc("guid", 1),
          IndexOptions()
            .name("guid-unique")
            .unique(true)
        )
        log.info("Added Index!")
      }
    ) {
      log.info("Index already exists")
    }
    val existingGuids = newsCollection.distinct("guid", String::class.java)
      .into(HashSet())

    var numDocuments = 0
    newsRssCollection.find(and(doc("enabled", ne(false))))
      .into(threadSafeList<Document>())
      .parallelStream()
      .forEach { dbDoc ->
        val url = dbDoc.getString("url")
        try {
          val response = httpGet(url)
          val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
          val document = docBuilder.parse(
            inputStream(
              response.body!!.toSmartString()
            )
          )
          val channels = document.getElementsByTagName("channel")
          log.debug("${channels.length} Channels on $url")
          IntRange(0, channels.length - 1)
            .map(channels::item)
            .parallelStream()
            .forEach { node ->
              try {
                log.debug(node.toXml())
                val items = node.getChildrenByTag("item")
                items.forEachIndexed { x, item ->
                  numDocuments++
                  val newsItem = doc("source", cleanDoc(dbDoc))
                    .add("time", System::currentTimeMillis)
                    .add("scrapeTime", System::currentTimeMillis)
                    .append("priorityUpdated", 0L)
                    .append("priority", -1)
                    .append("timesPrioritized", 0)
                    .append("visible", true)
                  val guid = item.addPropToDocument("guid", newsItem) {
                    log.debug("Could Not Find GUID on item! - {}", item.toXml())
                  }
                  if (guid == null || existingGuids.contains(guid)) {
                    return@forEachIndexed
                  }
                  val title = item.addPropToDocument("title", newsItem)
                  item.addPropToDocument("link", newsItem)
                  item.addPropToDocument("dc:creator", newsItem)
                  newsItem["indexInFeed"] = x
                  item.getFirstChildByTag("content:encoded")
                    .map { it.textContent }
                    .map { purgeHtml(it, excludedServers) }
                    .map(::forceHttps)
                    .ifPresent { value ->
                      newsItem.append("content", value)
                    }
                  item.addPropToDocument("description", newsItem)
                  item.addPropToDocument("pubDate", newsItem, { date ->
                    // See if date can be used for time
                    for (formatter in threadSafeList(
                      DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz"),
                      DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss OOOO")
                    )) {
                      try {
                        val parsed = LocalDateTime.parse(date, formatter)
                        val ms = parsed.toEpochSecond(ZoneOffset.UTC)
                        newsItem["time"] = ms
                        break
                      } catch (e: Exception) {
                        log.debug("Could not parse Date: $date", e)
                      }
                    }
                    date
                  }, {})
                  val categories = item.getChildrenByTag("category")
                    .map { it.textContent }
                  newsItem.append("categories", categories)

                  threadPool.submit {
                    try {
                      newsCollection.insertOne(newsItem)
                      log.info("Successfully Added News from ${dbDoc.getString("site")}: $title")
                    } catch (me: MongoWriteException) {
                      log.debug("Could not update document due to Static Size Limit: $guid")
                    } catch (e: Exception) {
                      log.warn("Could Not Add News Item to the Database", e)
                    }
                  }

                }
              } catch (e: Exception) {
                log.error("Error Processing News", e)
              }
            }
        } catch (xmlException: SAXParseException) {
          log.warn("Failed to parse xml on ${dbDoc.getString("site")}")
        } catch (e: Exception) {
          log.error("Error on $url", e)
        }
      }
    log.info("Found a total of $numDocuments Articles")
  }

  private fun Node.addPropToDocument(
    name: String,
    doc: Document,
    map: (String) -> String,
    notPresent: () -> Unit = {}
  ): String? {
    var s: String? = null
    this.getFirstChildByTag(name)
      .map { it.textContent }
      .map(map)
      .ifPresentOrElse({ value ->
        doc[name] = value
        s = value
      }, notPresent)
    return s
  }

  private fun Node.addPropToDocument(name: String, doc: Document, notPresent: () -> Unit = {}): String? =
    this.addPropToDocument(name, doc, { it }, notPresent)

  private fun Node.getChildrenByTag(tag: String): List<Node> =
    IntRange(0, this.childNodes.length - 1)
      .map(this.childNodes::item)
      .filter { it.nodeName == tag }

  private fun Node.getFirstChildByTag(tag: String): Optional<Node> =
    Optional.ofNullable(IntRange(0, this.childNodes.length - 1)
      .map(this.childNodes::item)
      .firstOrNull { it.nodeName == tag })

  private fun getExcludedServers(db: Database): Regex =
    Regex(
      "http.?://[^\"']*(${
        db.getCollection("lsrules")
          .distinct("server", String::class.java)
          .into(threadSafeList<String>()).joinToString(separator = "|")
      })"
    )

  private fun Node.toXml(): String {
    val sw = StringWriter()
    try {
      val t = TransformerFactory.newInstance().newTransformer()
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
      t.setOutputProperty(OutputKeys.ENCODING, Encoding.name())
      t.setOutputProperty(OutputKeys.INDENT, "yes")
      t.transform(DOMSource(this), StreamResult(sw))
    } catch (te: TransformerException) {
      log.error("nodeToString Transformer Exception", te)
    }

    return sw.toString()
  }

  private fun ResponseBody.toSmartString(): String {
    this.source().use {
      return it.readString(charset = it.readBomAsCharset(Encoding))
    }
  }
}
