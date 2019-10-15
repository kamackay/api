package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.IDatabase
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.UnauthorizedResponse


@Singleton
class TrackerRouter @Inject
internal constructor(private val validator: RequestValidator, db: IDatabase) : Router {

  override fun routes() {
    path("tracker") {
      validator.securePut("events") { ctx, body, user ->
        // TODO
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