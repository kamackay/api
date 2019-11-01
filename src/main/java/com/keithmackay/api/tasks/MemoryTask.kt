package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.minutes
import com.keithmackay.api.utils.getLogger
import com.keithmackay.api.utils.humanizeBytes
import com.keithmackay.api.utils.print

@Singleton
class MemoryTask @Inject
internal constructor() : Task() {
  private val log = getLogger(this::class)

  override fun time(): Long = minutes(1)

  override fun run() {
    val runtime = Runtime.getRuntime()
    val max = runtime.maxMemory()
    val available = runtime.freeMemory()
    val pct = available.toDouble() / max.toDouble()
    log.info("Remaining Allocated Memory: ${humanizeBytes(available)} (${(pct * 100).print(4)}%)")
  }
}