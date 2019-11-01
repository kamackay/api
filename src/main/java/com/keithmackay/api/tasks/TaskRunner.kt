package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.keithmackay.api.utils.getLogger
import com.keithmackay.api.utils.millisToReadableTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TaskRunner @Inject
internal constructor() {

  @Inject
  @JvmSuppressWildcards
  private lateinit var tasks: Set<Task>

  private val log = getLogger(this::class)
  private val scheduler = Executors.newScheduledThreadPool(2)

  fun start() =
      tasks.forEach {
        scheduler.scheduleAtFixedRate({
          val start = System.currentTimeMillis()
          try {
            it.run()
          } catch (e: Exception) {
            log.error("Error in ${it.javaClass.simpleName}", e)
          }
          val finish = System.currentTimeMillis()
          if (it.log()) {
            log.info("${it.javaClass.simpleName} finished in ${millisToReadableTime(finish - start)}")
          }
        }, 0, it.time(), TimeUnit.MILLISECONDS)
      }

}