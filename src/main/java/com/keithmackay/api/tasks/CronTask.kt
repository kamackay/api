package com.keithmackay.api.tasks

import org.quartz.CronScheduleBuilder
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.Job

abstract class CronTask : Job {
  open fun run() {
    this.execute(null)
  }

  abstract fun name(): String

  open fun cron(): String = "0 0 * * ?" // Once an hour

  open fun schedule(): CronScheduleBuilder = cronSchedule(this.cron())
}

class CronTimes {
  companion object {
    const val CRON_NEVER = "0 0 5 31 2 ?"
    const val CRON_EVERY_MORNING = "0 0 7 * * ?"

    fun minutes(mins: Int) = "0 */$mins * * * ?"

    fun seconds(secs: Int) = "*/$secs * * * * ?"
  }
}