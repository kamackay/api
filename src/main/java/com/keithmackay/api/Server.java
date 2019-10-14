package com.keithmackay.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.keithmackay.api.model.SuccessResponse;
import com.keithmackay.api.routes.*;
import com.keithmackay.api.utils.UtilsKt;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJson;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.keithmackay.api.utils.UtilsKt.getLogger;
import static io.javalin.apibuilder.ApiBuilder.get;

public class Server {

  private static final Logger log = getLogger(Server.class);

  private final Javalin app;
  private final Collection<Router> routers;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private final int port = Optional.ofNullable(System.getenv("PORT"))
      .map(Integer::parseInt)
      .orElse(9876);

  @Inject
  Server(final AuthRouter authRouter,
         final FilesRouter filesRouter,
         final GroceriesRouter groceriesRouter,
         final UserRouter userRouter) {
    this.app = Javalin.create(config -> {
      config.enableCorsForAllOrigins();
      config.requestLogger(UtilsKt::httpLog);
      JavalinJson.setToJsonMapper(this.gson::toJson);
      JavalinJson.setFromJsonMapper(this.gson::fromJson);
    }).exception(SuccessResponse.class,
            (e, ctx) -> ctx.status(e.getStatus()).result(e.getMessage()));
    this.routers = List.of(authRouter, filesRouter, groceriesRouter, userRouter);
  }

  void start() {
    this.app
        .routes(() -> {
          this.routers.forEach(Router::routes);
          get("ping", ctx -> {
            log.info("Received Ping Request");
            ctx.result("Hello");
          });
        })
        .start(port);
  }
}
