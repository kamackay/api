package com.keithmackay.api.users;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.keithmackay.api.db.DataSet;
import com.keithmackay.api.db.Database;

@Singleton
public class UserRepo {

  private final Database db;
  private final DataSet userCollection;

  @Inject
  UserRepo(final Database db) {
    this.db = db;
    this.userCollection = db.getCollection("users");
  }
}
