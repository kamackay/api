package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import org.bson.Document


@Singleton
class StatusRouter @Inject
internal constructor() : Router {

  override fun routes() {
    path("status") {
      get("/") {
        var status = it.sessionAttribute<Int>("status")
        if (status == null) {
          status = 200
        }
        it.status(status).result("I am currently serving status $status")
      }

      put("/") {
        try {
          val body = Document.parse(it.body())
          val status = body.getInteger("status")
          if (status >= 600) {
            throw BadRequestResponse()
          }
          it.sessionAttribute("status", status)
          it.status(status).result("Now serving $status")
        } catch (e: Exception) {
          it.status(400).result("Couldn't set status")
        }
      }

      get("/:status") {
        val code = it.pathParam("status", Int::class.java).get()
        if (code >= 600) {
          throw BadRequestResponse()
        }
        it.status(code).result("You Asked for $code")
      }
    }
  }

  override fun isHealthy(): Boolean = true

}