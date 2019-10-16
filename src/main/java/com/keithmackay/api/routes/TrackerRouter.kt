package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.model.SuccessResponse
import com.keithmackay.api.utils.*
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.UnauthorizedResponse
import org.bson.Document
import java.util.concurrent.CompletableFuture


@Singleton
class TrackerRouter @Inject
internal constructor(private val validator: RequestValidator, db: IDatabase) : Router {

  private val eventCollection = db.getCollection("events")

  override fun routes() {
    path("tracker") {
      validator.securePut("events") { ctx, body, user ->
        eventCollection.insertOne(body
            .cleanTo("url", "time", "data", "feature")
            .join(doc("time", System.currentTimeMillis())
                .add("user", user.username)))
        throw SuccessResponse()
      }

      validator.secureGet("events") { ctx, _, user ->
        if (!user.isAdmin) {
          throw UnauthorizedResponse()
        }
        val limit = Integer.parseInt(ctx.queryParam("pageSize"))
            .coerceAtMost(1000)
        val page = Integer.parseInt(ctx.queryParam("page", "0"))
        val query = Document.parse(ctx.queryParam("query"))

        ctx.json(CompletableFuture.supplyAsync {
          eventCollection
              .find(query)
              .skip(page * limit)
              .limit(limit)
              .into(threadSafeList<Document>())
              .map(::cleanDoc)
        })
      }
    }
  }
}