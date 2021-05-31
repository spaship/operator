package io.spaship.operator.event;

import io.smallrye.mutiny.Multi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/events")
public interface EventStreamSpec {
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Path("/stream")
    Multi<String> streamEvents();

    @GET
    @Path("/disable")
    @Produces(MediaType.TEXT_PLAIN)
    Response disableMessageSourcing();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/enable")
    Response enableMessageSourcing();
}
