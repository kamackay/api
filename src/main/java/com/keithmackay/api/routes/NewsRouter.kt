package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.utils.*
import com.mongodb.client.FindIterable
import io.javalin.apibuilder.ApiBuilder.*
import org.bson.Document
import org.bson.types.ObjectId
import java.util.concurrent.CompletableFuture

@Singleton
class NewsRouter @Inject
internal constructor(private val validator: RequestValidator, db: IDatabase) : Router {
  private val log = getLogger(this::class)

  private val newsCollection = db.getCollection("news")

  private val defaultNewsSort = doc("indexInFeed", 1)
      .add("time", -1)

  override fun routes() {
    path("news") {
      validator.secureGet("/", { ctx, _, user ->
        log.info("${user.username} Requests News")
        ctx.json(this.getAllNews())
      }, { ctx, _ ->
        // Unauthenticated Request. Sent the data, but don't be happy about it
        ctx.json(this.getAllNews())
      })

      get("/ids") {
        it.json(CompletableFuture.supplyAsync {
          newsCollection.distinct("_id", ObjectId::class.java)
              .map(ObjectId::toString)
              .into(threadSafeList())
        })
      }

      post("/ids") { ctx ->
        ctx.json(CompletableFuture.supplyAsync {
          val body = Document.parse(ctx.body())
          val ids = body.getList("ids", String::class.java)
          val docs = ids
              .map { doc("_id", eq(ObjectId(it))) }
              .toTypedArray()
          newsCollection.find(
              or(*docs))
              .map(::cleanDoc)
              .mapNotNull { it }
        })
      }

      get("/id/:id") {
        it.json(CompletableFuture.supplyAsync {
          val id = it.pathParam("id")
          newsCollection.find(doc("_id", ObjectId(id)))
              .map(::cleanDoc)
              .first()
        })
      }

      validator.secureGet("/site/:site", { ctx, _, user ->
        val siteName = ctx.pathParam("site")
        log.info("${user.username} requests all news for site '$siteName'")
        ctx.json(this.getNewsForSite(siteName))
      })

      validator.secureGet("/after/:time", { ctx, _, user ->
        val time = ctx.pathParam("time", Long::class.java).get()
        log.info("${user.username} requests all news after '$time'")
        ctx.json(this.getNewsAfter(time))
      }) { ctx, _ ->
        val time = ctx.pathParam("time", Long::class.java).get()
        log.info("Anonymous requests all news after '$time'")
        ctx.json(this.getNewsAfter(time))
      }

      validator.secureGet("/search/:text", { ctx, _, user ->
        val time = ctx.pathParam("time", Long::class.java).get()
        log.info("${user.username} requests all news after '$time'")
        ctx.json(this.getNewsAfter(time))
      })
    }
  }

  private fun getAllNews() = CompletableFuture.supplyAsync {
    newsCollection.find()
        .sort(defaultNewsSort)
        .limit(1000)
        .bundle()
  }

  private fun getNewsForSite(name: String) = CompletableFuture.supplyAsync {
    newsCollection.find(doc("site", name))
        .sort(defaultNewsSort)
        .bundle()
  }

  private fun getNewsAfter(time: Long) = CompletableFuture.supplyAsync {
    newsCollection.find(doc("time", gt(time)))
        .sort(defaultNewsSort)
        .bundle()
  }
}

private fun FindIterable<Document>.bundle() =
    this.into(threadSafeList<Document>())
        .map(::cleanDoc)
