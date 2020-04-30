package com.keithmackay.api.tasks

import org.quartz.Job

abstract class CronTask : Job {
  open fun run() {
    this.execute(null)
  }

  val CRON_NEVER = "0 0 5 31 2 ?"
  val CRON_EVERY_MORNING = "0 0 2 * * ?"

  abstract fun name(): String

  open fun schedule(): String = "0 0 * * ?" // Once an hour
}