package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.services.AdBlockService
import com.keithmackay.api.tasks.CronTimes.Companion.hours
import org.quartz.JobExecutionContext

@Singleton
class BlockRuleCacheTask
@Inject internal constructor(
        private val adBlockService: AdBlockService
) : CronTask() {
    override fun name(): String = "BlockRuleCacheTask"

    override fun execute(context: JobExecutionContext?) {
        adBlockService.doCacheSync("cron")
    }

    override fun cron(): String = hours(1)
}