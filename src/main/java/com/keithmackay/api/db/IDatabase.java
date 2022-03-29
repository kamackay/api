package com.keithmackay.api.db;

import com.google.inject.ImplementedBy;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

@ImplementedBy(Database.class)
public interface IDatabase {
    MongoCollection<Document> getCollection(final String name);

    MongoCollection<Document> getCollection(final String db, final String name);

    org.jongo.MongoCollection getJongoCollection(final String name);

    String getConnectionString();
}
