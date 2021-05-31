package io.spaship.operator.event;

import io.spaship.operator.service.EventSourcing;
import io.vertx.mutiny.core.Vertx;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import java.util.Optional;

@Dependent
public class EventConfiguration {

    private final Vertx vertx;
    private final Optional<Boolean> enabled;

    public EventConfiguration(Vertx vertx,
                              @ConfigProperty(name = "event.sourcing.enabled") Optional<Boolean> enabled) {
        this.vertx = vertx;
        this.enabled = enabled;
    }

    @Produces
    @Singleton
    public EventSourcing eventSourcingService() {
        return new EventSourcing(vertx, enabled);
    }

}
