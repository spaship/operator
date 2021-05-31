package io.spaship.operator.event;

import io.smallrye.mutiny.Multi;
import io.spaship.operator.resource.EventStreamSpec;
import io.spaship.operator.service.EventSourcing;
import io.vertx.mutiny.core.Vertx;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class EventStreamResource implements EventStreamSpec {

    private final EventSourcing eventSourcing;
    private final Vertx vertx;

    public EventStreamResource(EventSourcing ob,
                               Vertx vertx) {
        this.eventSourcing = ob;
        this.vertx = vertx;
    }

    @Override
    public Multi<String> streamEvents() {
        return vertx.eventBus()
                .consumer(EventSourcing.getBusAddress())
                .toMulti()
                .map(item -> item.body().toString());
    }

    @Override
    public Response disableMessageSourcing() {
        eventSourcing.setToggleEnabled(false);
        return Response.accepted("sourcing disabled").build();
    }

    @Override
    public Response enableMessageSourcing() {
        eventSourcing.setToggleEnabled(true);
        return Response.accepted("sourcing enabled").build();
    }
}
