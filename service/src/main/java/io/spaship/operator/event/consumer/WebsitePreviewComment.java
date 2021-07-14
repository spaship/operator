package io.spaship.operator.event.consumer;

import io.quarkus.vertx.ConsumeEvent;
import io.spaship.operator.webhook.WebhookService;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebsitePreviewComment {

    private static final Logger LOG = LoggerFactory.getLogger(WebsitePreviewComment.class);

    @ConsumeEvent(value = WebhookService.BUS_ADDRESS, blocking = true)
    public void logWebsiteEvents(JsonObject event){

    }


}
