package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import java.util.*

@Singleton
class PingTask @Inject
internal constructor() : Task() {

  private val port = Optional.ofNullable(System.getenv("PORT"))
      .map { Integer.parseInt(it) }
      .orElse(9876)

  override fun time(): Long = 1000

  override fun run() {
    khttp.get("http://localhost:$port/ping")
  }
}