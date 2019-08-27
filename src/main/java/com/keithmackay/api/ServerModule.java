package com.keithmackay.api;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.keithmackay.api.benchmark.Benchmark;
import com.keithmackay.api.benchmark.FunctionInterceptor;

public class ServerModule extends AbstractModule {
    @Override
    protected void configure() {
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Benchmark.class), new FunctionInterceptor());
    }

}

