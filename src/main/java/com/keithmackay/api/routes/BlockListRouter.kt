package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.Database
import com.keithmackay.api.utils.doc
import com.keithmackay.api.utils.getLogger
import com.keithmackay.api.utils.set
import com.keithmackay.api.utils.upsert
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.UnauthorizedResponse
import org.bson.Document

@Singleton
class BlockListRouter @Inject
internal constructor(private val validator: RequestValidator, private val db: Database) : Router {
  private val log = getLogger(this::class)
  private val lsCollection = db.getCollection("lsrules")

  override fun isHealthy(): Boolean = true

  override fun routes() {
    get("ls.json") { ctx ->
      val servers = lsCollection.find()
        .map { server ->
          server.getString("server")
        }.toList()

      ctx.json(servers)
    }

    path("files") {
      // Little Snitch Rules File
      get("rules.lsrules") { ctx ->
        log.info("Request for Little Snitch File")
        val rules = lsCollection.find()
          .mapIndexed { i, server ->
            val time = 1570476664f + i * 4.01
            doc("action", "deny")
              .append("creationDate", time)
              .append("modificationDate", time)
              .append("owner", "any")
              .append("process", "any")
              .append("remote-domains", server.getString("server"))

          }
        ctx.json(
          doc("name", "Keith's Little Snitch Rules")
            .append("rules", rules)
            .append("description", "List of Rules Generated by Keith MacKay's API")
        )
      }

      get("rules/list") { ctx ->
        log.info("Request to Count Domain Blocks")
        val rules = lsCollection.find()
          .map { it.getString("server") }
          .into(ArrayList())
        ctx.result(rules.joinToString(separator = "\n"))
      }

      get("rules/list.json") { ctx ->
        log.info("Request to Count Domain Blocks")
        val rules = lsCollection.find()
          .map { it.getString("server") }
          .into(ArrayList())
        ctx.json(rules)
      }

      get("rules/count") { ctx ->
        log.info("Request to Count Domain Blocks")
        val rules = lsCollection.find().count()
        log.info("There are $rules Current Blocks")
        ctx.json(Document().append("count", rules))
      }

      get("rules.list") { ctx ->
        log.info("Request for Domain Block Rules")
        val rules = lsCollection.find()
          .map {
            "0.0.0.0 ${it.getString("server")}"
          }
          .into(ArrayList())
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