package com.keithmackay.api;

import com.google.inject.Inject;
import com.keithmackay.api.routes.AuthRouter;
import com.keithmackay.api.routes.Router;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

public class Server {

  private static final Logger log = LoggerFactory.getLogger(Server.class);

  private final Javalin app;
  private final Collection<Router> routers;

  @Inject
  Server(final Javalin app, final AuthRouter authRouter) {
    this.app = app;
    this.routers = List.of(authRouter);
  }

  public void start() {
    this.app.enableMicrometer()
        .enableCorsForAllOrigins()
        .enableCaseSensitiveUrls()
        .error(404, ctx ->
            ctx.result(String.format("Could not find anything at \"%s\"" +
                " - What were you hoping to find?", ctx.url())))
        .requestLogger((ctx, time) -> {
          if (time > 10 || !"get".equals(ctx.method().toLowerCase())) {
            log.info("{} on '{}' from {} took {}ms",
                ctx.method(), ctx.path(), ctx.ip(), time);
          }
        })
        .routes(() -> this.routers.forEach(Router::routes)).start(9876);
  }
}
