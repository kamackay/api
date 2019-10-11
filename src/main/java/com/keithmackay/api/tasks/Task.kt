package com.keithmackay.api.tasks

import com.keithmackay.api.utils.getLogger

abstract class Task {
  private val log = getLogger(Task::class)

  fun start() = Thread {
    try {
      while (true) {
        this.run()
        Thread.sleep(this.time())
      }
    } catch (e: Exception) {
      log.error("Error in task", e)
    }
  }.start()

  abstract fun run()

  open fun time(): Long = 1000 * 60
}