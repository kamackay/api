package com.keithmackay.api.auth;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.keithmackay.api.db.Database;
import io.javalin.Context;
import org.bson.Document;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.keithmackay.api.utils.Utils.doc;

public class Authenticator {
  private final Database db;

  @Inject
  Authenticator(final Database db) {
    this.db = db;
  }

  public CompletableFuture<Document> check(
      final Context ctx, final Function<Document, ?> success) {
    return CompletableFuture.supplyAsync(() -> this.checkHelper(ctx, success));
  }

  private Document checkHelper(final Context ctx, final Function<Document, ?> success) {
    try {
      final String auth = ctx.header("Authorization");
      if (Strings.isNullOrEmpty(auth)) {
        throw new IllegalStateException("Need to provide Authorization");
      }

//      return optional(db.getCollection("api", "tokens")
//          .find(new Document("token", new Document("$eq", auth)))
//          .first())
//          .map(doc -> {
//            return optional(db.getCollection("api", "users")
//                .find(eq("username", doc.getString("username"))))
//                .orElse(null);
//          })
//          .orElse(null);

    } catch (Exception e) {
      return doc();
    }
    return null;
  }
}
