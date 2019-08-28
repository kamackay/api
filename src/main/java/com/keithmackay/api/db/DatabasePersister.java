package com.keithmackay.api.db;

import com.google.gson.Gson;
import com.google.inject.Inject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class DatabasePersister {
  private final Logger log = LoggerFactory.getLogger(DatabasePersister.class);

  @Inject
  DatabasePersister() {

  }

  void updated(final Map<ObjectId, Document> map, final String name) {
    try {
      Files.writeString(Paths.get(String.format("/db/%s", name)),
          new Gson().toJson(map));
    } catch (Exception e) {
      log.error("Could not write to file for {}", name, e);
    }
  }

}
