package io.websitecd.operator;


import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler counts requests
 */
public class RequestCountHandler implements Handler<RoutingContext> {

    long count = 0;

    public static RequestCountHandler create() {
        return new RequestCountHandler();
    }

    long lastCountPerSec = 0;

    public RequestCountHandler printPerf(Vertx vertx, String route, int periodInSec) {
        vertx.setPeriodic(periodInSec * 1000, e -> {
            System.out.println("Performance [" + route + "]: " + (count - lastCountPerSec) / periodInSec + " req/sec in last " + periodInSec + "sec");
            lastCountPerSec = count;
        });
        return this;
    }

    @Override
    public void handle(RoutingContext ctx) {
        count++;
        ctx.next();
    }

    public long getCount() {
        return count;
    }
}
