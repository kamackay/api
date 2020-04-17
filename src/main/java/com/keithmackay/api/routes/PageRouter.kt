package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.domn8.DOMn8
import com.keithmackay.api.domn8.nodes.DomNode
import com.keithmackay.api.domn8.nodes.HtmlBody
import com.keithmackay.api.domn8.nodes.HtmlBody.body
import com.keithmackay.api.domn8.nodes.elements.BreakEl.breakEl
import com.keithmackay.api.domn8.nodes.elements.CodeNode.codeEl
import com.keithmackay.api.domn8.nodes.elements.DivEl
import com.keithmackay.api.domn8.nodes.elements.HeaderEl.headerConfig
import com.keithmackay.api.domn8.nodes.elements.HeaderEl.headerEl
import com.keithmackay.api.domn8.nodes.elements.TextNode.textNode
import com.keithmackay.api.domn8.styles.CSS.css
import com.keithmackay.api.model.NewIPEmailModel
import com.keithmackay.api.utils.*
import com.keithmackay.api.utils.JavaUtils.toMap
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.UpdateOptions
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import io.javalin.apibuilder.ApiBuilder
import io.javalin.http.Context
import org.bson.Document
import org.json.JSONObject
import java.io.IOException
import java.util.*
import java.util.stream.Collectors

@Singleton
class PageRouter @Inject
internal constructor(db: IDatabase, private val creds: Credentials) : Router {

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
        val additional = Optional.ofNullable(body.get("additional", Document::class.java))
                .orElse(doc())
        val result = collection.updateOne(doc("ip", ip),
                doc("\$set", doc("ip", ip)
                        .join(doc()
                                .append("userAgent", ctx.userAgent()))
                        .join(additional))
                        .append("\$inc", doc("count", 1L)),
                UpdateOptions().upsert(true))
        if (result.matchedCount == 0L) {
            val info = Optional.ofNullable(getIpInfo(ip))
                    .orElseGet { this.defaultInfo(ip) }
            if (!ip.matches(Regex("^10\\."))) {
                sendEmailTo(info.getTitle(),
                        emailRenderer.renderIntoString(NewIPEmailModel(info = info,
                                additional = toMap(additional))),
                        "keith@keithm.io")
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

    private fun sendEmailTo(title: String, body: String, recipient: String) {
        val from = Email("api@keithm.io")
        val to = Email(recipient)
        val content = Content("text/html", body)
        val mail = Mail(from, title, to, content)
        val sg = SendGrid(creds.getString("sendgrid-key"))
        val request = Request()
        try {
            request.method = Method.POST
            request.endpoint = "mail/send"
            request.body = mail.build()
            val response: Response = sg.api(request)
            log.info("Response: {}; body: {}; headers: {}",
                    response.statusCode, response.body, response.headers)
        } catch (ex: IOException) {
            log.error("Error Sending Email", ex)
        }
    }

    private val emailRenderer = DOMn8.generic(NewIPEmailModel::class.java,
            { model: NewIPEmailModel ->
                val info = model.info
                body(HtmlBody.BodyConfig(),
                        listOf(
                                headerEl(headerConfig()
                                        .level(2)
                                        .text(info.getTitle())),
                                renderMainContent(model)
                        ) as List<DomNode<*>>?)
            }, "New IP")

    private fun renderMainContent(model: NewIPEmailModel): DivEl {
        val info = model.info
        return DivEl.divEl(DivEl.divConfig(),
                listOf(
                        row("New Page Load on Website"),
                        row("IP: ${info.ip}"),
                        row("${info.city}, ${info.region}, ${info.countryName} ${info.postal}"),
                        row("Coords: ${info.latitude} / ${info.longitude}"),
                        row("Organization: ${info.organization}"),
                        headerEl(headerConfig().text("Additional:").level(5)),
                        codeEl(JSONObject(model.additional).toString(2))
                )
        )
    }

    private fun row(line: String) =
            DivEl.divEl(DivEl.divConfig()
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

