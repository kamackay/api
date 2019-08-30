package com.keithmackay.api.routes;

import com.google.inject.Inject;
import com.keithmackay.api.db.Database;
import com.keithmackay.api.groceries.GroceryModule;
import com.keithmackay.api.model.GroceryItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.keithmackay.api.auth.TokenValidator.validateAuth;
import static io.javalin.apibuilder.ApiBuilder.*;

public class GroceriesRouter implements Router {
  private static final Logger log = LoggerFactory.getLogger(GroceriesRouter.class);

  private final GroceryModule module;
  private final Database db;

  @Inject
  GroceriesRouter(final GroceryModule module, final Database db) {
    this.module = module;
    this.db = db;
  }

  @Override
  public void routes() {
    path("/groceries", () -> {
      get("/items/:list", ctx -> validateAuth(ctx, db, user -> {
        ctx.json(module.getAllForList(ctx.pathParam("list")));
      }));
      put("/items/", ctx -> validateAuth(ctx, db, user ->
          module.addToList(ctx.bodyAsClass(GroceryItem.class), user)));
      put("/items/:list", ctx -> validateAuth(ctx, db, user ->
          module
              .addToList(ctx.pathParam("list"),
                  ctx.bodyAsClass(GroceryItem.class),
                  user)
              .thenAccept(response -> {
                log.info("Sending Response");
                ctx
                    .status(response.getStatus())
                    .result(response.getMessage());
              })));
    });
  }
}
