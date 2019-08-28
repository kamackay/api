package com.keithmackay.api.db;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.keithmackay.api.benchmark.Benchmark;

import java.util.HashMap;

@Singleton
public class Database {

  private final HashMap<String, DataSet> map;
  private final DataSetFactory dataSetFactory;

  @Inject
  Database(final DataSetFactory dataSetFactory) {
    this.dataSetFactory = dataSetFactory;
    map = new HashMap<>();
  }

  @Benchmark(limit = 10, paramName = true)
  public DataSet getCollection(final String name) {
    DataSet collection = map.getOrDefault(name, null);
    if (collection == null) {
      collection = dataSetFactory.create(name);
      map.put(name, collection);
    }
    return collection;
  }

}
