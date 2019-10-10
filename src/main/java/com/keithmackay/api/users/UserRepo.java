package com.keithmackay.api.users;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.keithmackay.api.db.Database;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

@Singleton
public class UserRepo {

  private final MongoCollection<Document> userCollection;

  @Inject
  UserRepo(final Database db) {
    this.userCollection = db.getCollection("users");
  }
}
