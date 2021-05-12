package io.spaship.operator.gitapi;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class GitlabApi implements GitApi {

    private static final Logger log = Logger.getLogger(GitlabApi.class);

    @Inject
    Vertx vertx;

    WebClient webClient;

    void startup(@Observes StartupEvent event) {
        log.infof("GitlabApi init.");
        webClient = WebClient.create(vertx, new WebClientOptions().setVerifyHost(false));
    }

    // TODO: Receive "website deployed/deleted" events and comment MR appropriately

    public Future<JsonObject> commentMergeRequest(String gitUrl, String projectId, String mergeRequestId, String accessToken, String body) {
        if (StringUtils.isEmpty(accessToken)) {
            return Future.succeededFuture(new JsonObject().put("status", "ignored"));
        }

        String commentApi = "/api/v4/projects/%s/merge_requests/%s/notes?body=%s";
        String requestURI = String.format(commentApi, projectId, mergeRequestId, body);

        URI uri = getURI(gitUrl);
        int port = uri.getPort();

        Promise<JsonObject> promise = Promise.promise();
        webClient.post(uri.getHost(), encodeURI(requestURI))
                .port(port).ssl(port == 433)
                .putHeader("PRIVATE-TOKEN", accessToken)
                .send(ar -> {
                    if (ar.result().statusCode() != 200) {
                        log.errorf(ar.cause(), "Error commenting MR url=%s", requestURI);
                        promise.tryFail(ar.cause());
                    } else {
                        JsonObject result = ar.result().bodyAsJsonObject();
                        log.infof("Merge Request commented");
                        log.tracef("Merge request comment result=%s", result);
                        promise.tryComplete(result);
                    }
                });
        return promise.future();
    }

    public static URI getURI(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static String encodeURI(String requestURI) {
        try {
            return URLEncoder.encode(requestURI, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
