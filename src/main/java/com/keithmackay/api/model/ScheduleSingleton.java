package com.keithmackay.api.model;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

public class ScheduleSingleton {

  private static Scheduler instance = null;

  public static Scheduler getInstance() throws SchedulerException {
    if (instance == null) {
      instance = StdSchedulerFactory.getDefaultScheduler();
    }

    return instance;
  }
}
