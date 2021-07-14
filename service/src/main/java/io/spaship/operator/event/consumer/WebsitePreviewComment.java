package io.spaship.operator.event.consumer;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.spaship.operator.webhook.WebhookService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;


import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Arrays;

import java.util.stream.Collectors;

@ApplicationScoped
public class WebsitePreviewComment {

    private static final Logger LOG = LoggerFactory.getLogger(WebsitePreviewComment.class);

    @Inject
    Vertx vertx;
    WebClient client;

    void onStart(@Observes StartupEvent ev) {
        client = WebClient.create(vertx);
    }


    @ConsumeEvent(value = WebhookService.BUS_ADDRESS, blocking = true)
    public void logWebsiteEvents(JsonObject event){

        String commentPostURL = constructCommentPostURL(event);

        JsonObject commentPayload = new JsonObject();
        commentPayload.put("body","RHML");

        LOG.info("comment post url {}, private token {}, comment body {}"
                ,commentPostURL,event.getString("apiAccessKey"),commentPayload);

        client
                .postAbs(commentPostURL)
                .putHeader("PRIVATE-TOKEN", event.getString("apiAccessKey"))
                .sendJsonObject(commentPayload,h-> {
                    if(h.failed())
                        System.out.println(h.cause());
                    System.out.println(h.result().body());
                });


    }


    private String constructCommentPostURL(JsonObject event) {

        var result  = new StringBuilder();
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
            case "CAT":
                break;
            default:
                break;


        }

        return result.toString();

    }

    private String extractBaseURL(String input){

        String [] part = input.split("/");
        var urlBuilder = new StringBuilder();
        Arrays.stream(part)
                .filter(item->!(item.equals("")))
                .collect(Collectors.toList())
                .subList(0,2)
                .forEach(elem-> {
                            if(elem.contains("http")){
                                urlBuilder.append(elem).append("//");
                                return;
                            }
                            urlBuilder.append(elem);
                            urlBuilder.append("/");
                        }
                );

        return urlBuilder.toString();

    }

}
