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
import com.keithmackay.api.tasks.*;

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
                UtilRouter.class,
                BlockListRouter.class,
                StatusRouter.class,
                SpeedTestRouter.class,
                WeatherRouter.class,
                TrackerRouter.class,
                GroceriesRouter.class,
                DnsOverHttpsRouter.class
        ).forEach(action -> routerBinder.addBinding().to(action));

        Multibinder<CronTask> cronTasks = Multibinder.newSetBinder(binder(), CronTask.class);
        Arrays.asList(
                //GoodMorningTask.class,
                //CryptoTask.class, // Not useful, just taking up cycles
                NextDnsUploadTask.class,
                //TestTask.class,
                NewsConversationCleanupTask.class,
                BlockRuleCacheTask.class,
                NewsCleanupTask.class
                //NewsPriorityTask.class
        ).forEach(task -> cronTasks.addBinding().to(task));

        Multibinder<Task> taskBinder = Multibinder.newSetBinder(binder(), Task.class);
        Arrays.asList(
                LsRuleTask.class,
                NewsTask.class,
                SessionCleanupTask.class,
                TokenCleanupTask.class
        ).forEach(task -> taskBinder.addBinding().to(task));
    }


    @Provides
    public Gson getGson() {
        return new GsonBuilder().create();
    }
}

