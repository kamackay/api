package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.email.EmailSender
import com.keithmackay.api.model.NewIPEmailModel
import com.keithmackay.api.utils.*
import com.keithmackay.api.utils.JavaUtils.Time.days
import com.keithmackay.api.utils.JavaUtils.toMap
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.UpdateOptions
import domn8.DOMn8
import domn8.nodes.DomNode
import domn8.nodes.HtmlBody
import domn8.nodes.HtmlBody.body
import domn8.nodes.elements.BreakEl.breakEl
import domn8.nodes.elements.DivEl
import domn8.nodes.elements.DivEl.divConfig
import domn8.nodes.elements.DivEl.divEl
import domn8.nodes.elements.HeaderEl.headerConfig
import domn8.nodes.elements.HeaderEl.headerEl
import domn8.nodes.elements.LinkEl
import domn8.nodes.elements.TextNode.textNode
import domn8.styles.CSS.css
import io.javalin.apibuilder.ApiBuilder
import io.javalin.http.Context
import org.bson.Document
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

val FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

@Singleton
class PageRouter @Inject
internal constructor(
  db: IDatabase,
  private val emailSender: EmailSender
) : Router {

  private val collection = db.getCollection("requests")
  private val log = getLogger(PageRouter::class)

  init {
    ensureIndex()
  }

  override fun routes() {
    ApiBuilder.path("page") {
      ApiBuilder.put("/", this::addRequest)
      ApiBuilder.get("/:ip") { ctx ->
        val ipEncoded = ctx.pathParam("ip")
        getRecordFromEncodedIp(ipEncoded).run {
          val builder = StringBuilder()
          this.forEach { pair ->
            if (pair.key == "_id" || pair.value == null) {
              return@forEach
            }
            builder.append(pair.key).append(" -> ")

            if (pair.value is Collection<*>) {
              val stringValue = (pair.value as Collection<*>).joinToString(separator = ", ") { it.toString() }
              builder.append(stringValue)
            } else {
              builder.append(pair.value?.toString() ?: "null")
            }
            builder.append("\n\n")
          }
          ctx.result(builder.toString())
        }
      }
      ApiBuilder.get("/:ip/json") { ctx ->
        val ipEncoded = ctx.pathParam("ip")
        getRecordFromEncodedIp(ipEncoded).run { ctx.json(this) }
      }
    }
  }

  private fun getRecordFromEncodedIp(encodedIp: String): Document {
    return try {
      val ip = URLDecoder.decode(encodedIp, StandardCharsets.UTF_8.toString())
      log.info("Finding all Page load records for $ip")

      Optional.of(collection.find(doc("ip", eq(ip))))
        .map { it.first() }
        .orElse(doc()) ?: doc()
    } catch (e: Exception) {
      log.error("Error looking up data", e)
      doc()
    }
  }

  private fun addRequest(ctx: Context) {
    ensureIndex()
    val emails = ArrayList<String>()
    emails.add(emailSender.mainUser())
    val body = Document.parse(ctx.body())
    log.info("Page Request: {}", body.toJson())
    val userAgent = ctx.userAgent() ?: ""
    if (Regex("""compatible;\s?\w{2,12}[Bb]ot""").containsMatchIn(userAgent)) {
      log.info("Indexer Request")
      return
    }
    val ip = Optional.of(body)
      .map { it.getString("ip") }
      .orElseGet(ctx::ip)
    Optional.of(body)
      .map { it.getList("additionalEmails", String::class.java) }
      .ifPresent { emails.addAll(it) }
    val existing = this.getExistingData(ip)
    val application = Optional.of(body)
      .map { it.getString("application") }
      .orElse("Main Page")
    val additional = Optional.ofNullable(body.get("additional", Document::class.java))
      .orElse(doc())
    val urls = existing.get("urls", ArrayList<Document>().javaClass) ?: ArrayList()
    urls.add(
      doc("url", additional.getString("url"))
        .append("time", System.currentTimeMillis())
        .append("date", FORMAT.format(Date()))
    )
    additional.remove("url")
    val result = collection.updateOne(
      doc("ip", ip),
      doc(
        "\$set", doc("ip", ip)
          .append("lastVisit", System.currentTimeMillis())
          .append("lastVisitDate", FORMAT.format(Date()))
          .append("firstVisit", existing.getLong("firstVisit") ?: System.currentTimeMillis())
          .append("firstVisitDate", existing.getString("firstVisitDate") ?: FORMAT.format(Date()))
          .join(
            doc()
              .append("urls", urls.filter(Objects::nonNull))
              .append("url", null)
              .append("userAgent", userAgent)
          )
          .drop("ip")
          .join(additional)
      )
        .append("\$inc", doc("count", 1L)),
      UpdateOptions().upsert(true)
    )
    val timeSinceLastVisit = System.currentTimeMillis() - (existing.getLong("lastVisit") ?: 0)
    log.info("Time since last visit is $timeSinceLastVisit ms")
    when {
      result.matchedCount == 0L -> {
        log.info("Access from new IP: $ip")
        val info = Optional.ofNullable(getIpInfo(ip))
          .orElseGet { this.defaultInfo(ip) }
        val model = NewIPEmailModel(
          info = info,
          additional = toMap(additional),
          application = application
        )
        if (!ip.matches(Regex("^10\\."))) {
          emailSender.send(
            model.getTitle(),
            emailRenderer.renderIntoString(model),
            emails
          )
        }
        ctx.result("OK")
        collection.updateOne(doc("ip", ip), doc("\$set", info.toMongo()))
      }

      timeSinceLastVisit > days(1) -> {
        log.info("Access from Old IP: $ip")
        val info = Optional.ofNullable(getIpInfo(ip))
          .orElseGet { this.defaultInfo(ip) }
        val model = NewIPEmailModel(
          info = info,
          additional = toMap(additional),
          application = application
        )
        emailSender.send(
          model.getTitle(true),
          emailRenderer.renderIntoString(model),
          emailSender.mainUser()
        )
      }
      else -> {
        ctx.status(205).result("OK")
      }
    }
  }

  private fun getExistingData(ip: String): Document {
    return collection.find(doc().append("ip", ip)).first() ?: doc().append("urls", ArrayList<String>())
  }

  private fun ensureIndex() {
    try {
      this.collection.createIndex(
        doc("ip", 1),
        IndexOptions().unique(true)
      )
    } catch (e: Exception) {
      // No-op
    }
  }

  private val emailRenderer = DOMn8.generic(
    NewIPEmailModel::class.java,
    { model: NewIPEmailModel ->
      body(
        HtmlBody.BodyConfig(),
        listOf(
          headerEl(
            headerConfig()
              .level(2)
              .text(model.getTitle())
          ),
          renderMainContent(model)
        ) as List<DomNode<*>>?
      )
    }, "New IP"
  )

  private fun renderMainContent(model: NewIPEmailModel): DivEl {
    val info = model.info
    return divEl(
      divConfig(),
      listOf(
        row("New Page Load on Website"),
        row("IP: ${info.ip}"),
        LinkEl.linkEl(
          LinkEl.LinkConfig()
            .text("View All Tracked Data")
            .url("https://api.keith.sh/page/${URLEncoder.encode(info.ip, StandardCharsets.UTF_8.toString())}")
        ),
        row("${info.city}, ${info.region}, ${info.countryName} ${info.postal}"),
        row("Coords: ${info.latitude} / ${info.longitude}"),
        row("Organization: ${info.organization}"),
        headerEl(headerConfig().text("Additional:").level(5)),
        renderAsJson(JSONObject(model.additional))
      )
    )
  }

  private fun renderAsJson(json: JSONObject): DivEl {
    val jsonString = json.toString(4)
    val elements = mutableListOf<DomNode<*>>()

    jsonString.split("\n")
      .map {
        var spaces = 0
        it.replace(Regex.fromLiteral("$\\s*")) { result ->
          spaces += result.value.length
          "&nbsp;"
        }
        divEl(
          divConfig()
            .styles(
              css()
                .set("padding-left", "${spaces * 10}px")
                .set("display", "block")
            ),
          listOf(textNode(it), breakEl())
        )
      }
      .forEach { elements.add(it) }

    return divEl(
      divConfig()
        .styles(
          css()
            //.set("background-color", "#333")
            .set("border-radius", "5px")
            .set("font-family", "monospace")
        ),
      elements
    )
  }


  private fun row(line: String) =
    divEl(
      divConfig()
        .styles(css().setValue("display", "block")),
      listOf(textNode(line), breakEl())
    )

  override fun isHealthy() = true

  private fun defaultInfo(ip: String) = IPInfo(
    ip = ip,
    city = "Unknown",
    region = "Unknown",
    regionCode = "Unknown",
    country = "Unknown",
    countryCode = "Unknown",
    countryName = "Unknown",
    postal = "Unknown",
    latitude = 0.0,
    longitude = 0.0,
    timezone = "Unknown",
    organization = "Unknown"
  )
}
