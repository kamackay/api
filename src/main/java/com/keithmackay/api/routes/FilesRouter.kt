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
import io.javalin.http.NotFoundResponse
import io.javalin.http.UnauthorizedResponse

@Singleton
class FilesRouter @Inject
internal constructor(private val validator: RequestValidator, db: Database) : Router {
  private val log = getLogger(this::class)
  private val lsCollection = db.getCollection("lsrules")

  override fun routes() {
    get("favicon.ico") { ctx ->
      val stream = this::class.java.classLoader.getResourceAsStream("files/favicon.ico")
      if (stream != null) {
        ctx.result(stream).contentType("image/x-icon")
      } else {
        NotFoundResponse("Could Not Find Favicon")
      }
    }

    path("files") {
      // Empty File just to test credentials
      get("secret.txt") {
        validator.validateThen { user ->
          it.status(200).json(doc("hello", user.getString("username")))
        }.handle(it)
      }

      validator.secureGet("secret.json", { ctx, _, user ->
        ctx.status(200)
            .json(doc("username", user.username))
      })

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
        ctx.json(doc("name", "Keith's Little Snitch Rules")
            .append("rules", rules)
            .append("description", "List of Rules Generated by Keith MacKay's API"))
      }

      validator.securePost("addRule", { ctx, body, user ->
        if (user.admin) {
          val server = body.getString("server")
          val result = lsCollection.updateOne(doc("server", server),
              set(doc("server", server)
                  .append("time", System.currentTimeMillis())),
              upsert())
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