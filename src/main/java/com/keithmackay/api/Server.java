package com.keithmackay.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.keithmackay.api.db.Database;
import com.keithmackay.api.model.InvalidAuthenticationResponse;
import com.keithmackay.api.model.SuccessResponse;
import com.keithmackay.api.routes.*;
import com.keithmackay.api.utils.UtilsKt;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJson;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.nosql.mongodb.MongoSessionDataStoreFactory;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionHandler;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.keithmackay.api.ConstantsKt.authSessionAttribute;
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

  private final String dbConnectionString;

  @Inject
  Server(final Database db,
         final AuthRouter authRouter,
         final FilesRouter filesRouter,
         final GroceriesRouter groceriesRouter,
         final UserRouter userRouter) {
    this.dbConnectionString = db.getConnectionString();
    this.app = Javalin.create(config -> {
      config.enableCorsForAllOrigins();
      config.requestLogger(UtilsKt::httpLog);
      JavalinJson.setToJsonMapper(this.gson::toJson);
      JavalinJson.setFromJsonMapper(this.gson::fromJson);
      config.sessionHandler(() -> {
        try {
          return getSessionHandler();
        } catch (Exception e) {
          log.error("Error Building Mongo Session Handler", e);
          return null;
        }
      });
    }).exception(SuccessResponse.class,
        (e, ctx) -> ctx.status(e.getStatus()).result(e.getMessage())
    ).exception(InvalidAuthenticationResponse.class,
        (e, ctx) -> {
          // Remove any existing Auth Data on the Session
          ctx.sessionAttribute(authSessionAttribute(), null);
          ctx.status(e.getStatus()).result(e.getMessage());
        }
    );
    this.routers = List.of(authRouter, filesRouter, groceriesRouter, userRouter);
  }

  void start() {
    this.app
        .routes(() -> {
          this.routers.forEach(Router::routes);
          get("ping", ctx -> {
            //log.info("Received Ping Request");
            ctx.result("Hello");
          });
        })
        .start(port);
  }

  private SessionHandler getSessionHandler() throws Exception {
    SessionHandler sessionHandler = new SessionHandler();
    SessionCache sessionCache = new DefaultSessionCache(sessionHandler);
    sessionCache.setSessionDataStore(
        this.getSessionDataStore().getSessionDataStore(sessionHandler)
    );
    sessionHandler.setSessionCache(sessionCache);
    sessionHandler.setHttpOnly(true);
    // make additional changes to your SessionHandler here
    return sessionHandler;
  }

  private MongoSessionDataStoreFactory getSessionDataStore() {
    MongoSessionDataStoreFactory mongoSessionDataStoreFactory = new MongoSessionDataStoreFactory();
    mongoSessionDataStoreFactory.setConnectionString(this.dbConnectionString);
    mongoSessionDataStoreFactory.setDbName("api");
    mongoSessionDataStoreFactory.setCollectionName("sessiondata");
    return mongoSessionDataStoreFactory;
  }

}
