package io.spaship.operator.event.consumer;

import io.quarkus.vertx.ConsumeEvent;
import io.spaship.operator.webhook.WebhookService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.stream.Collectors;

@ApplicationScoped
public class WebsitePreviewComment {

    private static final Logger LOG = LoggerFactory.getLogger(WebsitePreviewComment.class);

    final Vertx vertx;
    WebClient client;

    public WebsitePreviewComment(Vertx vertx) {
        this.vertx = vertx;
        client = WebClient.create(vertx);
    }


    @ConsumeEvent(value = WebhookService.BUS_ADDRESS, blocking = true)
    public void processPreviewEvent(JsonObject event) {

        String commentPostURL = constructCommentPostURL(event);

        var commentPayload = new JsonObject();
        commentPayload.put("body", markdownPreviewURLs(event));

        LOG.debug("comment post url {}, private token {}, comment body {}"
                , commentPostURL, event.getString("apiAccessKey"), commentPayload);
        var auth = apiAccessToken(event);

        postComment(commentPostURL, commentPayload, auth);

        LOG.debug("website preview event consumed");
    }

    private void postComment(String commentPostURL, JsonObject commentPayload, JsonObject auth) {
        client.postAbs(commentPostURL)
                .putHeader(auth.getString("key"), auth.getString("value"))
                .sendJsonObject(commentPayload, this::handleResponse);
    }

    private void handleResponse(AsyncResult<HttpResponse<Buffer>> handler) {
        if (handler.failed())
            LOG.error("failed to post route in Git discussion {}", handler.cause().getMessage());

        if (handler.result().statusCode() >= 200 && handler.result().statusCode() <= 205) {
            LOG.info("route posted in discussion");
        } else {
            LOG.error("failed to post the comment response code {} | message {} | response body {} , "
                    , handler.result().statusCode(), handler.result().statusMessage()
                    , handler.result().bodyAsJsonObject());
        }
    }


    private String constructCommentPostURL(JsonObject event) {

        var result = new StringBuilder();
        switch (event.getString("repoType")) {

            case "GITLAB":
                result.append(extractBaseURL(event.getString("projectUrl"), 0, 2))
                        .append("api/v4/projects/")
                        .append(event.getString("projectId"))
                        .append("/")
                        .append("merge_requests/")
                        .append(event.getString("prId"))
                        .append("/")
                        .append("notes");
                break;
            case "GITHUB":
                result.append(event.getString("projectUrl").replace("pulls", "issues"))
                        .append("/comments");
                break;
            default:
                break;


        }

        return result.toString();

    }

    private JsonObject apiAccessToken(JsonObject event) {

        var header = new JsonObject();

        switch (event.getString("repoType")) {
            case "GITLAB":
                header.put("key", "PRIVATE-TOKEN");
                header.put("value", event.getString("apiAccessKey"));
                break;
            case "GITHUB":
                header.put("key", "Authorization");
                header.put("value", "Bearer ".concat(event.getString("apiAccessKey")));
                break;
            default:
                break;

        }


        return header;
    }

    private String extractBaseURL(String input, int start, int end) {

        String[] part = input.split("/");
        var urlBuilder = new StringBuilder();
        Arrays.stream(part)
                .filter(item -> !(item.equals("")))
                .collect(Collectors.toList())
                .subList(start, end)
                .forEach(elem -> {
                            if (elem.contains("http")) {
                                urlBuilder.append(elem).append("//");
                                return;
                            }
                            urlBuilder.append(elem);
                            urlBuilder.append("/");
                        }
                );

        return urlBuilder.toString();

    }

    private String markdownPreviewURLs(JsonObject event) {

        var previewHosts = event.getJsonArray("previewHosts");
        var markDownComment = new StringBuilder();
        markDownComment.append("following are the preview urls <br/>");
        previewHosts.forEach(host -> {
            var obj = (JsonObject) host;
            markDownComment.append("[").append(obj.getString("env")).append("]")
                    .append("(http://").append(obj.getString("host")).append(")")
                    .append("<br/>");
        });
        return markDownComment.toString();
    }

}
