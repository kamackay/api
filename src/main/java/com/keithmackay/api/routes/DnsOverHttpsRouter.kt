package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.services.AdBlockService
import com.keithmackay.api.utils.getLogger
import com.keithmackay.api.utils.json
import io.javalin.apibuilder.ApiBuilder.get
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

const val TYPE_HEADER = "application/dns-json"

@Singleton
class DnsOverHttpsRouter @Inject
internal constructor(
  private val adBlockService: AdBlockService
) : Router {
  private val log = getLogger(this::class)
  override fun routes() {
    get("/dns-query") { ctx ->
      ctx.header("content-type", TYPE_HEADER)
      val host = ctx.queryParam("name")
      if (host == null) {
        ctx.status(400).result("")
        return@get
      }

      if (adBlockService.isBlocked(host)) {
        ctx.status(400).result("")
        return@get
      }

      val response = lookup(host)
      log.info("Lookup Response: $response")
      val body = response.body
      if (body == null) {
        ctx.status(response.code).result("")
        return@get
      }
      ctx.status(response.code).json(body.json())
    }
  }

  private fun lookup(host: String): Response {
    val urlBuilder = "https://8.8.8.8/dns-query".toHttpUrlOrNull()!!.newBuilder()
    urlBuilder.addQueryParameter("name", host)
    val client = OkHttpClient()
    val request: Request = Request.Builder()
      .url(urlBuilder.build())
      .header("accept", TYPE_HEADER)
      .build()
    return client.newCall(request).execute()
  }

  override fun isHealthy() = true
}