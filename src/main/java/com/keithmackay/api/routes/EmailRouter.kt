package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.IDatabase
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post

@Singleton
class EmailRouter @Inject
internal constructor(private val validator: RequestValidator, db: IDatabase) : Router {

  override fun routes() {
    path("email") {
      post("/") {ctx ->

      }
    }
  }

  fun sendEmail() {
    
  }

  override fun isHealthy() = true
}