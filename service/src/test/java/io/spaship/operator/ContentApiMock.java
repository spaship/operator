package io.spaship.operator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.jboss.logging.Logger;

public class ContentApiMock extends AbstractVerticle {

    private static final Logger log = Logger.getLogger(ContentApiMock.class);

    final int port;
    HttpServer server;

    RequestCountHandler apiListCount = RequestCountHandler.create();
    RequestCountHandler updateThemeCount = RequestCountHandler.create();
    RequestCountHandler updateRootCount = RequestCountHandler.create();
    RequestCountHandler updateSearchCount = RequestCountHandler.create();
    RequestCountHandler updateSharedCount = RequestCountHandler.create();


    public ContentApiMock(int port) {
        this.port = port;
    }

    @Override
    public void start() {

        Router router = Router.router(vertx);

        router.route("/api/list")
                .handler(apiListCount)
                .handler(ctx -> ctx.response()
                        .putHeader("content-type", "application/json")
                        .end("[\"test1\", \"test2\"]"));
        router.route("/api/update/theme")
                .handler(updateThemeCount)
                .handler(ctx -> ctx.response().end("DONE"));
        router.route("/api/update/template")
                .handler(updateThemeCount)
                .handler(ctx -> ctx.response().end("DONE"));

        router.route("/api/update/search")
                .handler(updateSearchCount)
                .handler(ctx -> ctx.response().end("DONE"));

        router.route("/api/update/shared-components")
                .handler(updateSharedCount)
                .handler(ctx -> ctx.response().end("DONE"));

        router.route("/api/update/_root")
                .handler(updateRootCount)
                .handler(ctx -> ctx.response().end("DONE"));

        server = vertx.createHttpServer()
                .exceptionHandler(t -> log.error("Error", t))
                .requestHandler(router)
                .listen(port);
        log.infof("Mock started port=%s", port);

    }

    @Override
    public void stop() throws Exception {
        super.stop();
        server.close();
    }

    public long getApiListCount() {
        return apiListCount.getCount();
    }

    public long getApiUpdateThemeCount() {
        return updateThemeCount.getCount();
    }

    public long getApiUpdateRootCount() {
        return updateRootCount.getCount();
    }

    public long getApiUpdateSearchCount() {
        return updateSearchCount.getCount();
    }

    public long getApiUpdateSharedCount() {
        return updateSharedCount.getCount();
    }

    public void reset() {
        apiListCount.reset();
        updateThemeCount.reset();
        updateRootCount.reset();
        updateSearchCount.reset();
        updateSharedCount.reset();
    }

}
