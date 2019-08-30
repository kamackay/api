package com.keithmackay.api.groceries;

import com.google.inject.Inject;
import com.keithmackay.api.db.Database;
import com.keithmackay.api.model.GroceryItem;
import com.keithmackay.api.model.HttpTextResponse;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static com.keithmackay.api.auth.AuthUtils.now;
import static com.keithmackay.api.db.Database.upsert;
import static com.keithmackay.api.utils.Utils.*;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class GroceryModule {
  private static final Logger log = LoggerFactory.getLogger(GroceryModule.class);
  private final MongoCollection<Document> groceryCollection;

  @Inject
  GroceryModule(final Database db) {
    this.groceryCollection = db.getCollection("api", "groceries");
  }

  private static Document map(final String list, final GroceryItem item, final Document user) {
    return doc()
        .append("list", list)
        .append("name", item.getName())
        .append("count", optional(item.getCount()).orElse(1))
        .append("addedAt", optional(item.getAddedAt())
            .map(time -> time == 0 ? null : time)
            .orElse(now().toEpochMilli()))
        .append("addedBy", optional(item.getAddedBy()).orElse(user.getString("username")))
        .append("removed", optional(item.isRemoved()).orElse(false));
  }

  public Collection<Document> getAllForList(final String list) {
    return this.groceryCollection
        .find(eq("list", list))
        .into(new ArrayList<>());
  }

  public CompletableFuture<HttpTextResponse> addToList(final String list,
                                                       final GroceryItem item,
                                                       final Document user) {
    return CompletableFuture.supplyAsync(() -> {
      final Bson filter = and(eq("list", list),
          eq("name", item.getName()));
      final HttpTextResponse.HttpTextResponseBuilder[] response = {HttpTextResponse.builder()};
      optional(this.groceryCollection
          .find(filter)
          .first())
          .map(doc -> {
            log.info(doc.toJson());
            return doc;
          })
          .ifPresent(document -> {
            item.setCount(item.getCount() + document.getInteger("count"));
            // There is already one of these in the list, increment the number of them that we need
            log.info(upsert(groceryCollection,
                doc().append("_id", document.getObjectId("_id")),
                merge(document, map(list, item, user))).toString());
            response[0] = response[0].status(200).message("Updated Item");
          })
          .orElse(() -> {
            log.info(upsert(groceryCollection, filter, map(list, item, user)).toString());
            response[0].status(200).message("Created Item");
          });
      return response[0].build();
    });
  }

  public CompletableFuture<HttpTextResponse> addToList(final GroceryItem item,
                                                       final Document user) {
    return addToList(item.getList(), item, user);
  }
}
