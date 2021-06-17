package io.spaship.operator.event.consumer;

import io.quarkus.vertx.ConsumeEvent;

import io.spaship.operator.event.EventSourcingEngine;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Optional;


@Singleton
/**
 * Primary consumer of website events
 * */
public class LogEvents {

    private static final Logger LOG = LoggerFactory.getLogger(LogEvents.class);
    private boolean toggleEnabled;



    public LogEvents(@ConfigProperty(name = "event.logging.enabled") Optional<Boolean> isEnabled) {
        setToggleEnabled(isEnabled.orElse(true));
    }

    @ConsumeEvent(value = EventSourcingEngine.BUS_ADDRESS, blocking = true)
    public void logWebsiteEvents(JsonObject event){
        if(toggleEnabled)
            LOG.info("{}",event);
    }

    public void setToggleEnabled(boolean set) {
        this.toggleEnabled = set;
        LOG.debug(">LogEvents toggleEnabled after set {}", this.toggleEnabled);
    }

}
