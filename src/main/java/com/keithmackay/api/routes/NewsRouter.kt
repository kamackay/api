package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.utils.*
import io.javalin.apibuilder.ApiBuilder.path
import org.bson.Document
import java.util.concurrent.CompletableFuture

@Singleton
class NewsRouter @Inject
internal constructor(private val validator: RequestValidator, db: IDatabase) : Router {
  private val log = getLogger(this::class)

  private val newsCollection = db.getCollection("news")

  override fun routes() {
    path("news") {
      validator.secureGet("/", { ctx, _, user ->
        log.info("${user.username} Requests News")
        ctx.json(this.getAllNews())
      }, { ctx, _ ->
        // Unauthenticated Request. Sent the data, but don't be happy about it
        ctx.json(this.getAllNews())
      })
    }
  }

  private fun getAllNews() =
      CompletableFuture.supplyAsync {
        newsCollection.find()
            .sort(doc("importance", 1)
                .add("time", -1))
            .into(threadSafeList<Document>())
            .map(::cleanDoc)
      }
}