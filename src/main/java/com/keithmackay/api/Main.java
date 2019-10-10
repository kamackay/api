package com.keithmackay.api;

import com.google.inject.Guice;

import static com.keithmackay.api.utils.UtilsKt.getLogger;

public class Main {
  public static void main(String[] args) {
    try {
      Guice.createInjector(new ServerModule()).getInstance(Server.class).start();
    } catch (Exception e) {
      getLogger(Main.class).error("Error Running API!", e);
    }
  }
}
