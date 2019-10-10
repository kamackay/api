package com.keithmackay.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.keithmackay.api.routes.AuthRouter;
import com.keithmackay.api.routes.Router;
import com.keithmackay.api.utils.UtilsKt;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJson;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

public class Server {

  private static final Logger log = LoggerFactory.getLogger(Server.class);

  private final Javalin app;
  private final Collection<Router> routers;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private final JsonParser parser = new JsonParser();

  @Inject
  Server(final AuthRouter authRouter) {
    this.app = Javalin.create(config -> {
      config.enableCorsForAllOrigins();
      config.requestLogger(UtilsKt::httpLog);
      JavalinJson.setToJsonMapper(this.gson::toJson);
      JavalinJson.setFromJsonMapper(this.gson::fromJson);
    });
    this.routers = List.of(authRouter);
  }

  void start() {
    this.app
        .routes(() -> this.routers.forEach(Router::routes))
        .start(9876);
  }
}
