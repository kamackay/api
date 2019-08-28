package com.keithmackay.api.routes;

import com.google.inject.Inject;
import com.keithmackay.api.benchmark.Benchmark;
import com.keithmackay.api.db.DataSet;
import com.keithmackay.api.db.Database;
import com.keithmackay.api.model.LoginModel;
import com.keithmackay.api.utils.Elective;
import com.keithmackay.api.utils.Utils;
import io.javalin.Context;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public class AuthRouter implements Router {
  private final static Logger log = LoggerFactory.getLogger(AuthRouter.class);
  private final Database db;
  private final DataSet authCollection;

  @Inject
  AuthRouter(final Database db) {
    this.db = db;
    this.authCollection = db.getCollection("auth");
  }

  @Override
  public void routes() {
    path("auth", () -> {
      post("login", this::login);
    });
  }

  @Benchmark(limit = 15)
  private void login(final Context ctx) {
    final LoginModel creds = ctx.bodyAsClass(LoginModel.class);
    final Elective<Document> documentElective = AuthUtils.login(this.authCollection, creds);
    documentElective
        .map(Utils::cleanDoc)
        .ifPresent(ctx::json)
        .orElse(() -> ctx.status(400));

  }
}
