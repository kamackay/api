package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.email.EmailSender
import com.keithmackay.api.utils.getLogger
import org.quartz.JobExecutionContext
import java.text.SimpleDateFormat
import java.util.*

@Singleton
class GoodMorningTask
@Inject internal constructor(
    private val emailSender: EmailSender
) : CronTask() {
  private val log = getLogger(this::class)

  override fun name(): String = "GoodMorning"

  override fun schedule() = CRON_NEVER // CRON_EVERY_MORNING // Every Morning

  override fun execute(context: JobExecutionContext?) {
    log.info("Good Morning")
    emailSender.send("Good Morning: ${today()}", "Good Morning", emailSender.mainUser())
  }

  private fun today(): String {
    return SimpleDateFormat("yyyy-MM-dd").format(Date())
  }
}