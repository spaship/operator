package io.spaship.operator.event.consumer;

import io.smallrye.mutiny.Multi;
import io.spaship.operator.event.EventSourcingEngine;
import io.vertx.mutiny.core.Vertx;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/events")

public class EventStreamingResource {

    private final Vertx vertx;

    public EventStreamingResource(Vertx vertx) {
        this.vertx = vertx;
    }



    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Path("/stream")
    public Multi<String> streamEvents() {
        return vertx.eventBus()
                .consumer(EventSourcingEngine.getBusAddress())
                .toMulti()
                .map(item -> item.body().toString());
    }

}