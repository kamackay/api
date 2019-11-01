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
import com.keithmackay.api.db.IDatabase;
import com.keithmackay.api.routes.AuthRouter;
import com.keithmackay.api.routes.GroceriesRouter;
import com.keithmackay.api.routes.NewsRouter;
import com.keithmackay.api.routes.Router;
import com.keithmackay.api.routes.StatusRouter;
import com.keithmackay.api.routes.TrackerRouter;
import com.keithmackay.api.routes.UserRouter;
import com.keithmackay.api.tasks.LsRuleTask;
import com.keithmackay.api.tasks.NewsPriorityTask;
import com.keithmackay.api.tasks.NewsTask;
import com.keithmackay.api.tasks.SessionCleanupTask;
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
    Multibinder<Router> routerBinder = Multibinder.newSetBinder(binder(), Router.class);
    routerBinder.addBinding().to(AuthRouter.class);
    routerBinder.addBinding().to(NewsRouter.class);
    routerBinder.addBinding().to(StatusRouter.class);
    routerBinder.addBinding().to(GroceriesRouter.class);
    routerBinder.addBinding().to(UserRouter.class);
    routerBinder.addBinding().to(TrackerRouter.class);

    Multibinder<Task> taskBinder = Multibinder.newSetBinder(binder(), Task.class);
    taskBinder.addBinding().to(LsRuleTask.class);
    taskBinder.addBinding().to(NewsTask.class);
    taskBinder.addBinding().to(NewsPriorityTask.class);
    taskBinder.addBinding().to(SessionCleanupTask.class);
    taskBinder.addBinding().to(TokenCleanupTask.class);
  }


  @Provides
  public Gson getGson() {
    return new GsonBuilder().create();
  }
}

