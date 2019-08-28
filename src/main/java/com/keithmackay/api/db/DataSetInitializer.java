package com.keithmackay.api.db;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class DataSetInitializer {
  private static final Logger log = LoggerFactory.getLogger(DataSetInitializer.class);

  @Inject
  DataSetInitializer() {
  }

  Map<ObjectId, Document> initialize(
      final String name,
      final Function<Document, Document> cleaner) {
    File file = new File(String.format("/db/%s", name));
    try {
      final Map<String, Document> documentMap = new Gson().fromJson(new FileReader(file),
          new TypeToken<Map<String, Document>>() {
          }.getType());
      final Map<ObjectId, Document> map = new HashMap<>();
      documentMap.entrySet().forEach(set ->
          map.put(new ObjectId(set.getKey()), cleaner.apply(set.getValue())));
      return map;
    } catch (FileNotFoundException e) {
      log.info("No Current Data Found stored for {}", name);
      return new HashMap<>();
    }
  }
}
