package com.keithmackay.api.db;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

import static com.keithmackay.api.auth.AuthUtils.urlEncode;

@Singleton
public class Database {
  public static final UpdateOptions UPSERT = new UpdateOptions().upsert(true);
  private static final Logger log = LoggerFactory.getLogger(Database.class);
  private final MongoClient client;

  @Inject
  Database() throws FileNotFoundException {
    log.info("Initializing Connection to MongoDB");
    final String uri =
        String.format("mongodb+srv://admin:%s@apicluster-tsly9.mongodb.net/test?retryWrites=true&w=majority",
            urlEncode(this.getPassword()));
    log.info(uri);
    this.client = new MongoClient(new MongoClientURI(uri));
  }

  public static UpdateResult upsert(
      final MongoCollection<Document> collection,
      final Bson filter,
      final Document newVal) {
    return collection.updateOne(filter, new Document("$set", newVal), UPSERT);
  }

  private String getPassword() throws FileNotFoundException {
    final Properties props = new Properties();
    final File file = new File("/home/api/creds");
    if (!file.exists()) throw new FileNotFoundException("Could not find Credentials file");
    try (final InputStream input = new FileInputStream(file.getAbsoluteFile())) {
      props.load(input);
    } catch (IOException e) {
      e.printStackTrace();
    }
    final String password = props.getProperty("pass", null);
    if (password == null) {
      throw new IllegalStateException("Could not find a Mongo Password");
    }
    return password;
  }

  public MongoCollection<Document> getCollection(final String dbName, final String name) {
    return this.client.getDatabase(dbName).getCollection(name);
  }
}
