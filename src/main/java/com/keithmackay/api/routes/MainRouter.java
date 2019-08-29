package com.keithmackay.api.routes;

import com.google.inject.Inject;
import com.keithmackay.api.db.Database;

import static com.keithmackay.api.auth.TokenValidator.validateAuth;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class MainRouter implements Router {

  private final Database db;

  @Inject
  MainRouter(final Database db) {
    this.db = db;
  }

  @Override
  public void routes() {
    path("/main", () -> {
      get("/test", ctx -> validateAuth(ctx, db, user -> {
        ctx.result("Hi, " + user.get("firstName"));
      }));
    });
  }
}
