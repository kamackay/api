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
import java.util.concurrent.CompletableFuture

@Singleton
class FilesRouter @Inject
internal constructor(private val validator: RequestValidator, private val db: Database) : Router {
    private val log = getLogger(this::class)

    override fun routes() {
        get("favicon.ico") { ctx ->
            val stream = this::class.java.classLoader.getResourceAsStream("files/favicon.ico")
            if (stream != null) {
                ctx.result(stream).contentType("image/x-icon")
            } else {
                NotFoundResponse("Could Not Find Favicon")
            }
        }

        get("gift-ideas") { ctx ->
            ctx.json(CompletableFuture.supplyAsync {
                db.getOrMakeCollection("gift-ideas", CreateCollectionOptions())
                        .find()
                        .map(::cleanDoc)
                        .mapNotNull { it }
            })
        }

        get("time") {
            it.result(System.currentTimeMillis().toString() + "\n")
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
        }
    }

    override fun isHealthy(): Boolean = true
}
