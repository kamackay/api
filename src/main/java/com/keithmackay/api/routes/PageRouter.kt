package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.utils.doc
import com.keithmackay.api.utils.getLogger
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.UpdateOptions
import io.javalin.apibuilder.ApiBuilder
import io.javalin.http.Context
import org.bson.Document
import java.util.*
import javax.mail.*
import javax.mail.Session.getInstance
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

@Singleton
class PageRouter @Inject
internal constructor(db: IDatabase) : Router {

    private val collection = db.getCollection("requests")
    private val log = getLogger(PageRouter::class)

    init {
        ensureIndex()
    }

    override fun routes() {
        ApiBuilder.path("page") {
            ApiBuilder.put("/", this::addRequest)
        }
    }

    private fun addRequest(ctx: Context) {
        ensureIndex()
        //val body = Document.parse(ctx.body())
        val ip = ctx.ip()
        val result = collection.updateOne(doc("ip", ip),
                doc("\$set", doc("ip", ip))
                        .append("\$inc", doc("count", 1L)),
                UpdateOptions().upsert(true))
        if (result.matchedCount == 0L) {
            //sendEmailTo("New IP!",
            //        "New User Has Signed into Application",
            //        "keith@keithm.io")
            log.info("New IP!")
        } else {
            ctx.status(205).result("Already existed")
        }
    }

    private fun ensureIndex() {
        try {
            this.collection.createIndex(
                    doc("ip", 1),
                    IndexOptions().unique(true))
        } catch (e: Exception) {
            // No-op
        }
    }

    override fun isHealthy() = true
}