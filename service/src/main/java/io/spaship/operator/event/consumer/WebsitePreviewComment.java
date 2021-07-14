package io.spaship.operator.event.consumer;

import io.quarkus.vertx.ConsumeEvent;
import io.spaship.operator.webhook.WebhookService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.UUID;
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
    public void logWebsiteEvents(JsonObject event) {

        String commentPostURL = constructCommentPostURL(event);

        var commentPayload = new JsonObject();
        commentPayload.put("body", markdownPreviewURLs(event));

        LOG.debug("comment post url {}, private token {}, comment body {}"
                , commentPostURL, event.getString("apiAccessKey"), commentPayload);

        client.postAbs(commentPostURL)
                .putHeader("PRIVATE-TOKEN", event.getString("apiAccessKey"))
                .sendJsonObject(commentPayload, h -> {
                    if (h.failed())
                        LOG.error("failed to post route in Git discussion {}", h.cause().getMessage());
                    LOG.info("route posted in discussion");
                });

        LOG.debug("website preview event consumed");
    }


    private String constructCommentPostURL(JsonObject event) {

        var result = new StringBuilder();
        switch (event.getString("repoType")) {

            case "GITLAB":
                result
                        .append(extractBaseURL(event.getString("projectUrl")))
                        .append("api/v4/projects/")
                        .append(event.getString("projectId"))
                        .append("/")
                        .append("merge_requests/")
                        .append(event.getString("prId"))
                        .append("/")
                        .append("notes");
                break;
            case "GITHUB":
                break;
            default:
                break;


        }

        return result.toString();

    }

    private String extractBaseURL(String input) {

        String[] part = input.split("/");
        var urlBuilder = new StringBuilder();
        Arrays.stream(part)
                .filter(item -> !(item.equals("")))
                .collect(Collectors.toList())
                .subList(0, 2)
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

    private String markdownPreviewURLs(JsonObject event){

        var previewHosts = event.getJsonArray("previewHosts");
        var markDownComment = new StringBuilder();
        markDownComment.append("following are the preview urls <br/>");
        previewHosts.forEach(host->{
            var obj = (JsonObject) host;
            markDownComment.append("[").append(obj.getString("env")).append("]")
                    .append("(http://").append(obj.getString("host")).append(")")
                    .append("<br/>");
        });
        return markDownComment.toString();
    }

}
