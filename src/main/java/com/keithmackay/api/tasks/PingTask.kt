package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import java.util.*

@Singleton
class PingTask @Inject
internal constructor() : Task() {

  override fun time(): Long = 1000 * 60

  override fun run() {
    khttp.get("http://keithmackay-api.herokuapp.com/ping")
  }
}