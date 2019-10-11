package com.keithmackay.api;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.keithmackay.api.tasks.TaskRunner;

import static com.keithmackay.api.utils.UtilsKt.getLogger;

public class Main {
  public static void main(String[] args) {
    try {
      final Injector injector = Guice.createInjector(new ServerModule());
      injector.getInstance(Server.class).start();
      injector.getInstance(TaskRunner.class).start();
    } catch (Exception e) {
      getLogger(Main.class).error("Error Running API!", e);
    }
  }
}
