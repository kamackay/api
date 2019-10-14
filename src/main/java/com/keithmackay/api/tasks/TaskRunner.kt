package com.keithmackay.api.tasks

import com.google.inject.Inject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TaskRunner @Inject
internal constructor(
    private val tasks: TaskList) {

  private val scheduler = Executors.newScheduledThreadPool(2)

  fun start() {
    tasks.forEach {
      scheduler.scheduleAtFixedRate(it::run, 0, it.time(), TimeUnit.MILLISECONDS)
    }
  }
}