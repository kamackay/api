package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.Database
import com.keithmackay.api.utils.cleanDoc
import com.keithmackay.api.utils.doc
import com.keithmackay.api.utils.getLogger
import com.mongodb.client.model.CreateCollectionOptions
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.NotFoundResponse
import org.bson.Document
import java.util.concurrent.CompletableFuture

@Singleton
class UtilRouter @Inject
internal constructor() : Router {
  private val log = getLogger(this::class)

  override fun routes() {
    get("/time") { ctx ->
      ctx.json(Document("time", System.currentTimeMillis()))
    }
  }

  override fun isHealthy(): Boolean = true
}
