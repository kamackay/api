package com.keithmackay.api.tasks;

import com.keithmackay.api.services.AdBlockService;
import com.keithmackay.api.utils.CredentialsGrabber;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.inject.Inject;

import static com.keithmackay.api.tasks.CronTimes.Companion;

@Slf4j
public class NextDnsUploadTask extends CronTask {

    private final AdBlockService adBlockService;

    @Inject
    NextDnsUploadTask(final AdBlockService adBlockService, final CredentialsGrabber grabber) {
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
        adBlockService.uploadToNextDns();
    }

}
