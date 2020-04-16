package com.keithmackay.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.keithmackay.api.benchmark.Benchmark;
import com.keithmackay.api.benchmark.FunctionInterceptor;
import com.keithmackay.api.db.Database;
import com.keithmackay.api.db.EphemeralDatabase;
import com.keithmackay.api.routes.*;
import com.keithmackay.api.tasks.LsRuleTask;
import com.keithmackay.api.tasks.NewsTask;
import com.keithmackay.api.tasks.SessionCleanupTask;
import com.keithmackay.api.tasks.Task;
import com.keithmackay.api.tasks.TokenCleanupTask;

import java.util.ArrayList;
import java.util.Arrays;

public class ServerModule extends AbstractModule {
  public static Injector getInjector() {
    return Guice.createInjector(new ServerModule());
  }

  @Override
  protected void configure() {
    bindInterceptor(Matchers.any(),
        Matchers.annotatedWith(Benchmark.class),
        new FunctionInterceptor());
    bind(Database.class).asEagerSingleton();
    bind(EphemeralDatabase.class).asEagerSingleton();
    Multibinder<Router> routerBinder = Multibinder.newSetBinder(binder(), Router.class);
    Arrays.asList(
        AuthRouter.class,
        NewsRouter.class,
        PageRouter.class,
        UserRouter.class,
        EmailRouter.class,
        FilesRouter.class,
        StatusRouter.class,
        TrackerRouter.class,
        GroceriesRouter.class
    ).forEach(action -> routerBinder.addBinding().to(action));

    Multibinder<Task> taskBinder = Multibinder.newSetBinder(binder(), Task.class);
    taskBinder.addBinding().to(LsRuleTask.class);
    taskBinder.addBinding().to(NewsTask.class);
    //taskBinder.addBinding().to(NewsPriorityTask.class);
    taskBinder.addBinding().to(SessionCleanupTask.class);
    taskBinder.addBinding().to(TokenCleanupTask.class);
  }


  @Provides
  public Gson getGson() {
    return new GsonBuilder().create();
  }
}

