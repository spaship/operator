package io.websitecd.operator;


import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.jboss.logging.Logger;

/**
 * Handler counts requests
 */
public class RequestCountHandler implements Handler<RoutingContext> {

    private static final Logger log = Logger.getLogger(RequestCountHandler.class);

    long count = 0;

    public static RequestCountHandler create() {
        return new RequestCountHandler();
    }

    @Override
    public void handle(RoutingContext ctx) {
        HttpServerRequest req = ctx.request();
        log.infof("received request: %s %s", req.method(), req.uri());
        count++;
        ctx.next();
    }

    public long getCount() {
        return count;
    }

    public void reset() {
        count = 0;
    }
}
