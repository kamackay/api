package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.EphemeralDatabase
import com.keithmackay.api.services.NewsService
import com.keithmackay.api.tasks.CronTimes.Companion.MIDNIGHT_EVERY_DAY
import com.keithmackay.api.utils.getLogger
import org.quartz.JobExecutionContext

@Singleton
class DailyNewsCleanupTask @Inject
internal constructor(private val newsService: NewsService) : CronTask() {
    private val log = getLogger(this::class)

    override fun cron(): String = MIDNIGHT_EVERY_DAY

    override fun name(): String = "DailyNewsCleanupTask"

    override fun execute(context: JobExecutionContext?) {
        log.info("Dropping News Database")
        newsService.deleteAll()
    }
}