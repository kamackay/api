package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.utils.add
import com.keithmackay.api.utils.cleanTo
import com.keithmackay.api.utils.doc
import com.keithmackay.api.utils.join
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.UnauthorizedResponse


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
      }

      validator.secureGet("events") { ctx, _, user ->
        if (!user.isAdmin) {
          throw UnauthorizedResponse()
        }

        // TODO
      }
    }
  }
}