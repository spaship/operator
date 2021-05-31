package io.spaship.operator.resource;

import io.smallrye.mutiny.Multi;
import javax.ws.rs.core.Response;

public interface EventStreamResourceSpec {

    Multi<String> streamEvents();
    Response disableMessageSourcing();
    Response enableMessageSourcing();
}
