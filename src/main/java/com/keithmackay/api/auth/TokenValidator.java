package com.keithmackay.api.auth;

import com.google.common.base.Strings;
import com.keithmackay.api.db.Database;
import com.keithmackay.api.utils.Elective;
import io.javalin.Context;
import org.bson.Document;

import javax.naming.AuthenticationException;
import java.util.function.Consumer;

public class TokenValidator {

  public static void validateAuth(
      final Context ctx,
      final Database db,
      final Consumer<Document> success,
      final Consumer<Exception> failure) {
    try {
      final String auth = ctx.header("Authorization");
      if (Strings.isNullOrEmpty(auth)) {
        throw new IllegalStateException("Need to provide Authorization");
      }

      Elective.ofNullable(db.getCollection("api", "tokens")
          .find(new Document("token", new Document("$eq", auth)))
          .first())
          .ifPresent(doc -> {
            Elective.ofNullable(db.getCollection("api", "users")
                .find(new Document("username", new Document("$eq", doc.getString("username"))))
                .first())
                .ifPresent(success)
                .orElse(() -> failure.accept(new IllegalStateException("Could not find user for the given token")));
          })
          .orElse(() -> failure.accept(new AuthenticationException("Could not find the given token")));

    } catch (Exception e) {
      failure.accept(e);
    }
  }

  public static void validateAuth(
      final Context ctx,
      final Database db,
      final Consumer<Document> success) {
    validateAuth(ctx, db, success, ex -> ctx.status(401).result(ex.getMessage()));
  }

}
