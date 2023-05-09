package com.keithmackay.api.tasks;

import com.keithmackay.api.services.AdBlockService;
import com.keithmackay.api.utils.Ratio;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.inject.Inject;

import java.util.concurrent.atomic.AtomicLong;

import static com.keithmackay.api.tasks.CronTimes.Companion;

@Slf4j
public class NextDnsUploadTask extends CronTask {

    private final AdBlockService adBlockService;
    private static final AtomicLong counter = new AtomicLong(0);

    @Inject
    NextDnsUploadTask(final AdBlockService adBlockService) {
        this.adBlockService = adBlockService;
    }

    @NotNull
    @Override
    public String cron() {
        return Companion.seconds(5);
    }

    @NotNull
    @Override
    public String name() {
        return "NextDnsUploadTask";
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Starting task {} to upload a server to NextDNS", counter.get());
        adBlockService.uploadToNextDns();
        if (counter.incrementAndGet() % 20 == 0) {
            final Ratio ratio = adBlockService.countNextDnsProgress();
            log.info("NextDNS has {} out of {} servers", ratio.getCount(), ratio.getTotal());
        }
    }

}
