package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.email.EmailSender
import com.keithmackay.api.model.NewIPEmailModel
import com.keithmackay.api.utils.*
import com.keithmackay.api.utils.JavaUtils.toMap
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.UpdateOptions
import io.javalin.apibuilder.ApiBuilder
import io.javalin.http.Context
import io.keithm.domn8.DOMn8
import io.keithm.domn8.nodes.DomNode
import io.keithm.domn8.nodes.HtmlBody
import io.keithm.domn8.nodes.HtmlBody.body
import io.keithm.domn8.nodes.elements.BreakEl.breakEl
import io.keithm.domn8.nodes.elements.DivEl
import io.keithm.domn8.nodes.elements.DivEl.divConfig
import io.keithm.domn8.nodes.elements.DivEl.divEl
import io.keithm.domn8.nodes.elements.HeaderEl.headerConfig
import io.keithm.domn8.nodes.elements.HeaderEl.headerEl
import io.keithm.domn8.nodes.elements.TextNode.textNode
import io.keithm.domn8.styles.CSS.css
import org.bson.Document
import org.json.JSONObject
import java.util.*

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
    }
  }

  private fun addRequest(ctx: Context) {
    ensureIndex()
    val body = Document.parse(ctx.body())
    val ip = Optional.of(body)
        .map { it.getString("ip") }
        .orElseGet(ctx::ip)
    val application = Optional.of(body)
        .map { it.getString("application") }
        .orElse("Main Page")
    val additional = Optional.ofNullable(body.get("additional", Document::class.java))
        .orElse(doc())
    val result = collection.updateOne(doc("ip", ip),
        doc("\$set", doc("ip", ip)
            .append("firstVisit", System.currentTimeMillis())
            .join(doc()
                .append("userAgent", ctx.userAgent()))
            .join(additional))
            .append("\$inc", doc("count", 1L)),
        UpdateOptions().upsert(true))
    if (result.matchedCount == 0L) {
      log.info("Access from new IP: $ip")
      val info = Optional.ofNullable(getIpInfo(ip))
          .orElseGet { this.defaultInfo(ip) }
      val model = NewIPEmailModel(info = info,
          additional = toMap(additional),
          application = application)
      if (!ip.matches(Regex("^10\\."))) {
        emailSender.send(model.getTitle(),
            emailRenderer.renderIntoString(model),
            emailSender.mainUser())
      }
      ctx.result("OK")
      collection.updateOne(doc("ip", ip), doc("\$set",
          info.toMongo()))
    } else {
      ctx.status(205).result("OK")
    }
  }

  private fun ensureIndex() {
    try {
      this.collection.createIndex(
          doc("ip", 1),
          IndexOptions().unique(true))
    } catch (e: Exception) {
      // No-op
    }
  }


  private val emailRenderer = DOMn8.generic(NewIPEmailModel::class.java,
      { model: NewIPEmailModel ->
        body(HtmlBody.BodyConfig(),
            listOf(
                headerEl(headerConfig()
                    .level(2)
                    .text(model.getTitle())),
                renderMainContent(model)
            ) as List<DomNode<*>>?)
      }, "New IP")

  private fun renderMainContent(model: NewIPEmailModel): DivEl {
    val info = model.info
    return divEl(divConfig(),
        listOf(
            row("New Page Load on Website"),
            row("IP: ${info.ip}"),
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
          divEl(divConfig()
              .styles(css()
                  .set("padding-left", "${spaces * 10}px")
                  .set("display", "block")),
              listOf(textNode(it), breakEl()))
        }
        .forEach { elements.add(it) }

    return divEl(divConfig()
        .styles(css()
            //.set("background-color", "#333")
            .set("border-radius", "5px")
            .set("font-family", "monospace")),
        elements)
  }


  private fun row(line: String) =
      divEl(divConfig()
          .styles(css().setValue("display", "block")),
          listOf(textNode(line), breakEl()))

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

