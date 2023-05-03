package com.keithmackay.api.tasks;

import com.keithmackay.api.services.AdBlockService;
import com.keithmackay.api.utils.CredentialsGrabber;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.inject.Inject;

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
        return "0 0 0 1-31/2 * * ?"; // Every Other day
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
