package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.utils.getLogger
import io.javalin.apibuilder.ApiBuilder.get
import kotlin.random.Random


@Singleton
class SpeedTestRouter @Inject
internal constructor(
) : Router {
  private val log = getLogger(this::class)
  override fun routes() {
    get("speed/{byteCount}") { ctx ->
      val byteCount = ctx.pathParam("byteCount").toInt()
      ctx.result(String(Random.Default.nextBytes(byteCount)))
    }
  }

  override fun isHealthy() = true
}