package io.spaship.operator.event;

import io.smallrye.mutiny.Multi;
import io.spaship.operator.resource.EventStreamResourceSpec;

import io.spaship.operator.service.EventSourcing;
import io.vertx.mutiny.core.Vertx;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/events")
public class EventStreamResource implements EventStreamResourceSpec {

    private final EventSourcing eventSourcing;
    private final Vertx vertx;

    public EventStreamResource(EventSourcing ob,
                               Vertx vertx) {
        this.eventSourcing = ob;
        this.vertx = vertx;
    }

    @Override
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Path("/stream")
    public Multi<String> streamEvents() {
        return vertx.eventBus()
                .consumer(EventSourcing.getBusAddress())
                .toMulti()
                .map(item -> item.body().toString());
    }

    @Override
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/disable")
    public Response disableMessageSourcing() {
        eventSourcing.setToggleEnabled(false);
        return Response.accepted("sourcing disabled").build();
    }

    @Override
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/enable")
    public Response enableMessageSourcing() {
        eventSourcing.setToggleEnabled(true);
        return Response.accepted("sourcing enabled").build();
    }
}
