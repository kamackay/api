package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.Database
import com.keithmackay.api.utils.*
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.UnauthorizedResponse
import org.bson.Document
import java.time.Duration

@Singleton
class BlockListRouter @Inject
internal constructor(private val validator: RequestValidator, private val db: Database) : Router {
  private val log = getLogger(this::class)
  private val lsCollection = db.getCollection("lsrules")
  private val rulesCache = Cacher<List<String>>(Duration.ofMinutes(15), "LS Block Hosts")

  override fun isHealthy(): Boolean = true

  private fun getDocuments() = rulesCache.get("all") {
    lsCollection.find()
      .into(ArrayList())
      .map { it.getString("server") }
  }

  override fun routes() {
    get("ls.json") { ctx ->
      val servers = getDocuments()
      ctx.json(servers)
    }

    path("files") {
      // Little Snitch Rules File
      get("rules.lsrules") { ctx ->
        log.info("Request for Little Snitch File")
        val rules = getDocuments()
          .mapIndexed { i, server ->
            val time = 1.57047667E9f + i * 4.01
            doc("action", "deny")
              .append("creationDate", time)
              .append("modificationDate", time)
              .append("owner", "any")
              .append("process", "any")
              .append("remote-domains", server)

          }
        ctx.json(
          doc("name", "Keith's Little Snitch Rules")
            .append("rules", rules)
            .append("description", "List of Rules Generated by Keith MacKay's API")
        )
      }

      get("rules/list") { ctx ->
        log.info("Request to Count Domain Blocks")
        val rules = getDocuments()
        ctx.result(rules.joinToString(separator = "\n"))
      }

      get("rules/list.json") { ctx ->
        log.info("Request to Count Domain Blocks")
        val rules = getDocuments()
        ctx.json(rules)
      }

      get("rules/count") { ctx ->
        log.info("Request to Count Domain Blocks")
        val rules = getDocuments().size
        log.info("There are $rules Current Blocks")
        ctx.json(Document().append("count", rules))
      }

      get("rules.list") { ctx ->
        log.info("Request for Domain Block Rules")
        val rules = getDocuments()
          .map {
            "0.0.0.0 $it"
          }
        ctx.result(rules.joinToString(separator = "\n"))
      }

      validator.securePost("addRule", { ctx, body, user ->
        if (user.admin) {
          val server = body.getString("server")
          val result = lsCollection.updateOne(
            doc("server", server),
            set(
              doc("server", server)
                .append("time", System.currentTimeMillis())
            ),
            upsert()
          )
          if (result.modifiedCount > 0 || result.upsertedId != null) {
            ctx.result("Added")
            log.info("Added $server to the Blocked Servers List")
          } else {
            ctx.status(400).result("Not Added")
            log.info("Failed to add $server to the list of Blocked Servers")
          }
        } else {
          throw UnauthorizedResponse()
        }
      })
    }
  }
}