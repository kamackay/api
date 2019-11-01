package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import org.bson.Document
import java.util.concurrent.atomic.AtomicInteger


@Singleton
class StatusRouter @Inject
internal constructor() : Router {

  private val currentStatus = AtomicInteger(200)

  override fun routes() {
    path("status") {
      get("/") {
        it.status(currentStatus.get()).result("I am currently serving status ${currentStatus.get()}")
      }

      put("/") {
        try {
          val body = Document.parse(it.body())
          val status = body.getInteger("status")
          currentStatus.set(status)
          it.status(status).result("Now serving $status")
        } catch (e: Exception) {
          it.status(400).result("Couldn't set status!")
        }
      }

      get("/:status") {
        val code = it.pathParam("status", Int::class.java).get()
        if (code > 600) {
          throw BadRequestResponse()
        }
        it.status(code).result("You Asked for $code")
      }
    }
  }

}