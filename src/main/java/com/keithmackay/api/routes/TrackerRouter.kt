package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.model.SuccessResponse
import com.keithmackay.api.model.User
import com.keithmackay.api.services.ImageTrackingService
import com.keithmackay.api.utils.*
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import io.javalin.http.UnauthorizedResponse
import org.bson.Document
import java.util.concurrent.CompletableFuture


@Singleton
class TrackerRouter @Inject
internal constructor(
        private val validator: RequestValidator,
        private val imageTrackingService: ImageTrackingService,
        db: IDatabase
) : Router {

    private val log = getLogger(this::class)

    private val eventCollection = db.getCollection("events")

    override fun routes() {
        path("tracker") {
            validator.securePut("events", this::addEvent) { ctx, body ->
                this.addEvent(ctx, body)
            }

            validator.secureGet("events", { ctx, _, user ->
                if (!user.admin) {
                    log.info("User '${user.username}' tried to access tracking events, but is not admin")
                    throw UnauthorizedResponse()
                }
                val limit = Integer.parseInt(ctx.queryParam("pageSize"))
                        .coerceAtMost(1000)
                val page = Integer.parseInt(ctx.queryParam("page") ?: "0")
                val query = try {
                    Document.parse(ctx.queryParam("query"))
                } catch (e: Exception) {
                    log.info("Invalid Query Supplied: ${ctx.queryParam("query")}")
                    doc()
                }

                ctx.json(eventCollection
                            .find(query)
                            .skip(page * limit)
                            .limit(limit)
                            .into(threadSafeList<Document>())
                            .map(::cleanDoc))
            })
        }

        get("/img/{id}") { ctx ->
            val recipients = (ctx.queryParam("additional") ?: "").split(",")
            imageTrackingService.triggerTrackerId(ctx.pathParam("id"), recipients)

            val stream = this::class.java.classLoader.getResourceAsStream("files/1px.png")
            if (stream != null) {
                ctx.result(stream).contentType("image/png")
            } else {
                NotFoundResponse("Could Not Find Favicon")
            }
        }
    }

    private fun addEvent(ctx: Context, body: Document, user: User? = null): Nothing {
        val additional = doc("time", System.currentTimeMillis())
        additional.run {
            if (user != null) {
                this.append("user", user.username)
            }
        }
        eventCollection.insertOne(
                body.cleanTo("url", "time", "data", "feature", "userAgent", "ip", "location")
                        .join(doc("ip", ctx.ip())
                                .add("userAgent", ctx::userAgent), false)
                        .join(additional, true)
        )
        throw SuccessResponse()
    }

    override fun isHealthy(): Boolean = true
}