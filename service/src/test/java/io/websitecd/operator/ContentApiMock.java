package io.websitecd.operator;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import org.jboss.logging.Logger;

public class ContentApiMock  extends AbstractVerticle {

    private static final Logger log = Logger.getLogger(ContentApiMock.class);

    int port;

    RequestCountHandler apiListCount = RequestCountHandler.create();
    RequestCountHandler apiUpdate1Count = RequestCountHandler.create();
    RequestCountHandler apiUpdate2Count = RequestCountHandler.create();


    public ContentApiMock(int port) {
        this.port = port;
    }

    @Override
    public void start() throws Exception {

        Router router = Router.router(vertx);

        router.route("/api/list")
                .handler(apiListCount)
                .handler(ctx -> ctx.response()
                        .putHeader("content-type", "application/json")
                        .end("[\"test1\", \"test2\"]"));
        router.route("/api/update/theme")
                .handler(apiUpdate1Count)
                .handler(ctx -> ctx.response().end("DONE"));

        router.route("/api/update/_root")
                .handler(apiUpdate2Count)
                .handler(ctx -> ctx.response().end("DONE"));

        vertx.createHttpServer()
                .exceptionHandler(t -> log.error("Error", t))
                .requestHandler(router)
                .listen(port);
        log.infof("Mock started port=%s", port);

    }

    public long getApiListCount() {
        return apiListCount.getCount();
    }
    public long getApiUpdateTest1() {
        return apiUpdate1Count.getCount();
    }
    public long getApiUpdateTest2() {
        return apiUpdate2Count.getCount();
    }

    public void reset() {
        apiListCount.reset();
        apiUpdate1Count.reset();
        apiUpdate2Count.reset();
    }

}
