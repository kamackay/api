package com.keithmackay.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.keithmackay.api.db.Database;
import com.keithmackay.api.model.InvalidAuthenticationResponse;
import com.keithmackay.api.model.SuccessResponse;
import com.keithmackay.api.routes.Router;
import com.keithmackay.api.utils.UtilsKt;
import io.javalin.Javalin;
import io.javalin.core.compression.Brotli;
import io.javalin.core.compression.Gzip;
import io.javalin.core.util.RouteOverviewPlugin;
import io.javalin.plugin.json.JavalinJson;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.nosql.mongodb.MongoSessionDataStoreFactory;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionHandler;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.keithmackay.api.ConstantsKt.authSessionAttribute;
import static com.keithmackay.api.utils.UtilsKt.getLogger;
import static io.javalin.apibuilder.ApiBuilder.get;

public class Server {

  private static final Logger log = getLogger(Server.class);

  private final Javalin app;
  private final Gson gson = new GsonBuilder()
      //.setPrettyPrinting()
      .create();
  private final int port = Optional.ofNullable(System.getenv("PORT"))
      .map(Integer::parseInt)
      .orElse(9876);
  private static final long BAD_REQUEST_LIMIT = 10000L;
  private final AtomicLong lastBadRequest = new AtomicLong(0);

  private final String dbConnectionString;
  private final Set<Router> routers;

  @Inject
  Server(final Database db,
         final Set<Router> routers) {
    this.routers = routers;
    this.dbConnectionString = db.getConnectionString();
    this.app = Javalin.create(config -> {
      config.enableCorsForAllOrigins();
      config.requestLogger((ctx, time) -> {
        UtilsKt.httpLog(ctx, time);
        if (time > BAD_REQUEST_LIMIT) {
          log.error("Bad Request Time. Reporting.");
          this.lastBadRequest.set(System.currentTimeMillis());
        }
      });
      JavalinJson.setToJsonMapper(this.gson::toJson);
      JavalinJson.setFromJsonMapper(this.gson::fromJson);
      config.registerPlugin(new RouteOverviewPlugin("overview"));
      //config.compressionStrategy(new Brotli(4), new Gzip(7));
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
    ).routes(() -> {
      routers.forEach(Router::routes);
      get("ping", ctx -> {
        final boolean routersHealthy = routers.stream().allMatch(Router::isHealthy);
        final Runtime runtime = Runtime.getRuntime();
        final double memoryRatio = ((double) runtime.freeMemory()
            / (double) Math.min(runtime.maxMemory(), runtime.totalMemory()))
            * 100;
        final long now = System.currentTimeMillis();
        if (now - this.lastBadRequest.get() < 1000 || memoryRatio < 5 || !routersHealthy) {
          log.error("Reporting Ping as Error State - {}% memory available", memoryRatio);
          ctx.status(500).result("Not Working");
        } else {
          ctx.status(200).result("Working Fine");
        }
      });
    });
  }

  void start() {
    this.app
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
    mongoSessionDataStoreFactory.setCollectionName("session_data");
    return mongoSessionDataStoreFactory;
  }

}
