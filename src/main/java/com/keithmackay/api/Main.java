package com.keithmackay.api;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.keithmackay.api.model.ScheduleSingleton;
import com.keithmackay.api.tasks.CronTask;
import com.keithmackay.api.tasks.TaskRunner;
import com.keithmackay.api.utils.GuiceJobFactory;
import com.nixxcode.jvmbrotli.common.BrotliLoader;
import lombok.Getter;
import lombok.val;
import org.apache.logging.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import java.util.Set;

import static com.keithmackay.api.utils.UtilsKt.*;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class Main {
    private static final Logger log = getLogger(Main.class);

    public static void main(String[] args) {
        try {
            final Runtime runtime = Runtime.getRuntime();
            log.info("CPU Cores Available: {}", runtime.availableProcessors());
            log.info("Memory Limit: {}", humanizeBytes(runtime.maxMemory()));
            final Injector injector = Guice.createInjector(new ServerModule());
            verifyBrotli();
            configureCron(injector);
            injector.getInstance(Server.class).start();
            injector.getInstance(TaskRunner.class).start();
        } catch (Exception e) {
            getLogger(Main.class).error("Error Running API!", e);
        }
    }

    private static void verifyBrotli() {
        try {
            final boolean available = BrotliLoader.isBrotliAvailable();
            if (!available) {
                throw new Exception("Can't load Brotli");
            }

        } catch (Exception e) {
            log.error("Couldn't load Brotli", e);
        }
    }

    private static void configureCron(final Injector injector) throws SchedulerException {
        val log = getLogger("Main");
        Scheduler scheduler = ScheduleSingleton.getInstance();
        scheduler.setJobFactory(injector.getInstance(GuiceJobFactory.class));
        injector.getInstance(TaskHolder.class).getTasks().forEach(task -> {
            try {
                val details = newJob()
                        .ofType(task.getClass())
                        .withIdentity(task.name(), "cron")
                        .build();
                ScheduleSingleton.getInstance()
                        .scheduleJob(details,
                                newTrigger()
                                        .withIdentity(task.name(), "cron")
                                        .withSchedule(task.schedule())
                                        .forJob(details)
                                        .startNow()
                                        .build());
            } catch (SchedulerException e) {
                log.error("Error With Scheduler", e);
            }
        });

        defer(() -> {
            try {
                scheduler.start();
            } catch (Exception e) {
                log.error("Error Starting Cron!", e);
            }
        }, 2000);
    }


    public static class TaskHolder {
        @Getter
        private final Set<CronTask> tasks;

        @Inject
        TaskHolder(final Set<CronTask> tasks) {
            this.tasks = tasks;
        }
    }
}
