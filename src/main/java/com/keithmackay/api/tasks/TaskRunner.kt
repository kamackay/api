package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.keithmackay.api.utils.getLogger
import com.keithmackay.api.utils.millisToReadableTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TaskRunner @Inject
internal constructor(
    private val tasks: TaskList) {

  private val log = getLogger(this::class)
  private val scheduler = Executors.newScheduledThreadPool(2)

  fun start() =
      tasks.forEach {
        scheduler.scheduleAtFixedRate({
          val start = System.currentTimeMillis()
          it.run()
          val finish = System.currentTimeMillis()
          log.info("${it.javaClass.simpleName} finished in ${millisToReadableTime(finish - start)}")
        }, 0, it.time(), TimeUnit.MILLISECONDS)
      }

}