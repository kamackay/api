package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.model.SuccessResponse
import com.keithmackay.api.model.User
import com.keithmackay.api.utils.*
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import org.bson.Document
import java.util.concurrent.CompletableFuture


@Singleton
class TrackerRouter @Inject
internal constructor(private val validator: RequestValidator, db: IDatabase) : Router {

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
        val page = Integer.parseInt(ctx.queryParam("page", "0"))
        val query = try {
          Document.parse(ctx.queryParam("query"))
        } catch (e: Exception) {
          log.info("Invalid Query Supplied: ${ctx.queryParam("query")}")
          doc()
        }

        ctx.json(CompletableFuture.supplyAsync {
          eventCollection
              .find(query)
              .skip(page * limit)
              .limit(limit)
              .into(threadSafeList<Document>())
              .map(::cleanDoc)
        })
      })
    }
  }

  private fun addEvent(ctx: Context, body: Document, user: User? = null): Nothing {
    val additional = doc("time", System.currentTimeMillis())
    additional.run {
      if (user != null) {
        this.add("user", user.username)
      }
    }
    eventCollection.insertOne(body
        .cleanTo("url", "time", "data",
            "feature", "userAgent", "ip", "location")
        .join(doc("ip", ctx.ip())
            .add("userAgent", ctx.userAgent()), false)
        .join(additional, true))
    throw SuccessResponse()
  }
}