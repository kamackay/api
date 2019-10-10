package com.keithmackay.api.routes;

import com.google.inject.Inject;
import com.keithmackay.api.benchmark.Benchmark;
import com.keithmackay.api.db.Database;
import com.keithmackay.api.model.LoginModel;
import com.keithmackay.api.utils.UtilsKt;
import com.mongodb.client.MongoCollection;
import io.javalin.http.Context;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public class AuthRouter implements Router {
  private final static Logger log = LoggerFactory.getLogger(AuthRouter.class);
  private final MongoCollection<Document> tokenCollection;
  private final MongoCollection<Document> userCollection;

  @Inject
  AuthRouter(final Database db) {
    this.tokenCollection = db.getCollection("tokens");
    this.userCollection = db.getCollection("users");
  }

  @Override
  public void routes() {
    path("auth", () -> {
      post("login", this::login);
      post("logout", this::logout);
    });
  }

  @Benchmark(limit = 15)
  private void login(final Context ctx) {
    final LoginModel creds = ctx.bodyAsClass(LoginModel.class);
    final Optional<Document> documentElective = AuthUtils.login(this.userCollection, this.tokenCollection, creds);
    documentElective
        .map(UtilsKt::cleanDoc)
        .ifPresentOrElse(ctx::json, () -> ctx.status(400));

  }

  @Benchmark(limit = 15)
  private void logout(final Context ctx) {
    ctx.status(200).result("Successful");
  }
}
