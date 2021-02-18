package io.websitecd.operator.rest;

import io.quarkus.vertx.web.Route;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.websitecd.operator.webhook.WebhookService;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class WebHookResource {

    public static final String CONTEXT = "api/webhook/";

    private static final Logger log = Logger.getLogger(WebHookResource.class);

    @Inject
    WebhookService webhookService;

    public static List<String> apis(String rootPath) {
        List<String> apis = new ArrayList<>();
        apis.add(rootPath + CONTEXT);
        return apis;
    }

    @Route(methods = HttpMethod.POST, path = "/api/webhook", produces = "application/json")
    public void webhook(RoutingContext rc) {
        Buffer body = rc.getBody();
        HttpServerRequest request = rc.request();
        log.infof("webhook called from url=%s", request.remoteAddress());

        webhookService.handleRequest(request, body)
                .onSuccess(ar -> rc.response().end(ar.toBuffer()))
                .onFailure(err -> {
                    if (err instanceof WebApplicationException) {
                        WebApplicationException exc = (WebApplicationException) err;
                        rc.response()
                                .setStatusCode(exc.getResponse().getStatus())
                                .end(exc.getMessage());
                    } else {
                        rc.fail(err);
                    }
                });
    }

    public static String getHeader(HttpServerRequest request, String name) {
        List<String> headers = request.headers().getAll(name);
        if (headers == null || headers.size() == 0) {
            return null;
        }
        return StringUtils.trimToNull(headers.get(0));
    }

}
