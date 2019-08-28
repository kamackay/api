package com.keithmackay.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.matcher.Matchers;
import com.keithmackay.api.benchmark.Benchmark;
import com.keithmackay.api.benchmark.FunctionInterceptor;
import com.keithmackay.api.db.DataSet;
import com.keithmackay.api.db.DataSetFactory;
import com.keithmackay.api.db.DataSetImpl;

public class ServerModule extends AbstractModule {
  public static Injector getInjector() {
    return Guice.createInjector(new ServerModule());
  }

  @Override
  protected void configure() {
    bindInterceptor(Matchers.any(),
        Matchers.annotatedWith(Benchmark.class),
        new FunctionInterceptor());
    install(new FactoryModuleBuilder()
        .implement(DataSet.class, DataSetImpl.class)
        .build(DataSetFactory.class));

  }

  @Provides
  public Gson getGson() {
    return new GsonBuilder().create();
  }

}

