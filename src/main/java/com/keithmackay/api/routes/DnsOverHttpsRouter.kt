package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.utils.getLogger
import io.javalin.apibuilder.ApiBuilder.get

@Singleton
class DnsOverHttpsRouter @Inject
internal constructor(
) : Router {
  private val log = getLogger(this::class)
  override fun routes() {
    get("/dns-query") {ctx ->
      
    }
  }

  override fun isHealthy() = true
}