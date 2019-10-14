package com.keithmackay.api;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.keithmackay.api.tasks.TaskRunner;
import org.apache.logging.log4j.Logger;

import static com.keithmackay.api.utils.UtilsKt.getLogger;
import static com.keithmackay.api.utils.UtilsKt.humanizeBytes;

public class Main {
  public static void main(String[] args) {
    try {
      final Logger log = getLogger(Main.class);
      final Runtime runtime = Runtime.getRuntime();
      log.info("CPU Cores Available: {}", runtime.availableProcessors());
      log.info("Memory Limit: {}", humanizeBytes(runtime.maxMemory()));
      final Injector injector = Guice.createInjector(new ServerModule());
      injector.getInstance(Server.class).start();
      injector.getInstance(TaskRunner.class).start();
    } catch (Exception e) {
      getLogger(Main.class).error("Error Running API!", e);
    }
  }
}
