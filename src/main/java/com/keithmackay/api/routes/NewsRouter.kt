package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.EphemeralDatabase
import com.keithmackay.api.services.NewsService
import com.keithmackay.api.utils.*
import com.mongodb.MongoException
import com.mongodb.client.FindIterable
import io.javalin.apibuilder.ApiBuilder.*
import org.bson.Document
import org.bson.types.ObjectId
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class NewsRouter @Inject
internal constructor(
    private val validator: RequestValidator,
    private val db: EphemeralDatabase,
    private val newsService: NewsService
) : Router {
  private val log = getLogger(this::class)
  private val healthy = AtomicBoolean(true)

  override fun routes() {
    path("news") {
      validator.secureGet("/", { ctx, _, user ->
        log.info("${user.username} Requests News")
        ctx.json(this.getAllNews())
      }, { ctx, _ ->
        // Unauthenticated Request. Sent the data, but don't be happy about it
        ctx.json(this.getAllNews())
      })

      delete("/") {
        newsService.deleteAll()
        it.status(204).result("Done")
      }

      delete("/id/{guid}") {
        newsService.delete(it.pathParam("guid"))
        it.status(204).result("Done")
      }

      get("/top/{count}") { ctx ->
        val countStr = ctx.pathParam("count")
        var count = 5;
        try {
          count = Integer.parseInt(countStr)
        } catch (e: Exception) {
          log.error("Couldn't parse to int", e)
        }
        ctx.json(newsService.getDaysTopNews(count))
      }

      get("/ids") {
        try {
          it.json(
              db.getCollection("news")
                  .distinct("_id", ObjectId::class.java)
                  .map(ObjectId::toString)
                  .into(threadSafeList())
          )
        } catch (e: MongoException) {
          this.healthy.set(false)
          throw e
        }
      }

      get("/sort") { ctx ->
        ctx.json(newsService.defaultNewsSort)
      }

      get("sources") { ctx ->
        ctx.json(newsService.getSources())
      }

      post("/ids") { ctx ->
        try {
          val body = Document.parse(ctx.body())
          val ids = body.getList("ids", String::class.java)
          val docs = ids
              .map { doc("_id", eq(ObjectId(it))) }
              .toTypedArray()
          ctx.json(db.getCollection("news").find(
              or(*docs)
          )
              .sort(newsService.defaultNewsSort)
              .limit(1000)
              .map(::cleanDoc)
              .mapNotNull { it })
        } catch (e: MongoException) {
          this.healthy.set(false)
          throw e
        }
      }

      get("/id/{id}") {
        val id = it.pathParam("id")
        it.json(
            db.getCollection("news").find(doc("_id", ObjectId(id)))
                .map(::cleanDoc)
                .first()
                .orEmpty()
        )
      }

      get("ids_after/{time}") { ctx ->
        val time = ctx.pathParamAsClass("time", Long::class.java).get()
        log.info("Anonymous requests all news after '$time'")
        ctx.json(db.getCollection("news").find(doc("time", gt(time)))
            .sort(newsService.defaultNewsSort)
            .limit(1000)
            .map { it.getObjectId("_id").toString() }
            .into(threadSafeList()))
      }

      validator.secureGet("/site/{site}", { ctx, _, user ->
        val siteName = ctx.pathParam("site")
        log.info("${user.username} requests all news for site '$siteName'")
        ctx.json(this.getNewsForSite(siteName))
      })

      validator.secureGet("/after/{time}", { ctx, _, user ->
        val time = ctx.pathParamAsClass("time", Long::class.java).get()
        log.info("${user.username} requests all news after '$time'")
        ctx.json(this.getNewsAfter(time))
      }) { ctx, _ ->
        val time = ctx.pathParamAsClass("time", Long::class.java).get()
        log.info("Anonymous requests all news after '$time'")
        ctx.json(this.getNewsAfter(time, 800))
      }

      validator.secureGet("/search/{text}", { ctx, _, user ->

      })
    }
  }

  private fun getAllNews() = CompletableFuture.supplyAsync {
    newsService.getAll().bundle()
  }.get()

  private fun getNewsForSite(name: String) = CompletableFuture.supplyAsync {
    db.getCollection("news").find(doc("site", name))
        .sort(newsService.defaultNewsSort)
        .limit(1000)
        .bundle()
  }.get()

  private fun getNewsAfter(time: Long) = getNewsAfter(time, 1000)

  private fun getNewsAfter(time: Long, limit: Int) = CompletableFuture.supplyAsync {
    db.getCollection("news").find(doc("time", gt(time)))
        .sort(newsService.defaultNewsSort)
        .limit(limit)
        .bundle()
  }.get()

  override fun isHealthy(): Boolean = this.healthy.get()
}

private fun FindIterable<Document>.bundle() =
    this.into(threadSafeList<Document>())
        .map(::cleanDoc)
