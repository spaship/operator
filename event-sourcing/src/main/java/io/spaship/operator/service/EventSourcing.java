package io.spaship.operator.service;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class EventSourcing {

    private static final Logger LOG = LoggerFactory.getLogger(EventSourcing.class);
    private static final String BUS_ADDRESS = "crud-event-source";
    private final Vertx vertx;
    private boolean toggleEnabled;

    public EventSourcing(Vertx vertx, Optional<Boolean> isEnabled) {
        this.vertx = vertx;
        setToggleEnabled(isEnabled.orElse(true));
    }

    public static String getBusAddress() {
        return BUS_ADDRESS;
    }

    public void publishMessage(String payload) {
        LOG.debug("toggleEnabled vale is {}", toggleEnabled);
        if (!toggleEnabled)
            return;
        JsonObject messageBody = buildMessageBody(payload);
        LOG.debug("sourcing message {}", messageBody);
        vertx.eventBus().publish(BUS_ADDRESS, messageBody); // sourcing message to the bus
    }

    private JsonObject buildMessageBody(String payload) {
        var jsonPayload = new JsonObject();
        jsonPayload.put("id", UUID.randomUUID().toString()); // duplicate event detection
        jsonPayload.put("payload", transformMessage(payload));
        jsonPayload.put("time", LocalDateTime.now().toString()); // required to maintain for every event
        return jsonPayload;
    }

    private Object transformMessage(String input) {
        try {
            return new JsonObject(input);
        } catch (DecodeException decodeException) {
            LOG.error("failed to parse string into json detected {}", decodeException.getMessage());
            return input;
        }
    }

    public void setToggleEnabled(boolean set) {
        this.toggleEnabled = set;
        LOG.debug(">toggleEnabled after set {}", this.toggleEnabled);
    }

}
