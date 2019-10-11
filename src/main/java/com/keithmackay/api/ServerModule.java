package com.keithmackay.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.MultibindingsScanner;
import com.keithmackay.api.benchmark.Benchmark;
import com.keithmackay.api.benchmark.FunctionInterceptor;
import com.keithmackay.api.db.IDatabase;
import com.keithmackay.api.tasks.LsRuleTask;
import com.keithmackay.api.tasks.Task;
import com.keithmackay.api.tasks.TaskList;
import com.keithmackay.api.tasks.TokenCleanupTask;

public class ServerModule extends AbstractModule {
  public static Injector getInjector() {
    return Guice.createInjector(new ServerModule());
  }

  @Override
  protected void configure() {
    bindInterceptor(Matchers.any(),
        Matchers.annotatedWith(Benchmark.class),
        new FunctionInterceptor());
    bind(IDatabase.class).asEagerSingleton();
    bind(TaskList.class).asEagerSingleton();
  }


  @Provides
  public Gson getGson() {
    return new GsonBuilder().create();
  }
}

