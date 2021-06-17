# Website Events

### Event Lists

| Event Name | Description | Is Available |
| -----------|-------------|--------------|
|WEBSITE_CREATE|website create event|yes|
|WEBSITE_UPDATE|website update event|yes|
|WEBSITE_DELETE|website delete event|yes|
|PREVIEW_CREATE|website preview create event|yes|
|PREVIEW_UPDATE|website preview update event|yes|
|PREVIEW_DELETE|website preview delete event|yes|


### How to Consume Events

All the events are published in this bus `crud-event-source`, there are two ways to consume events within the application 

1. `vertx.eventBus().consumer("crud-event-source",handler->...)` for more details on browse [this page](https://vertx.io/docs/vertx-core/java/#_registering_handlers)
2. use `@ConsumeEvent` annotation. for more details on browse [this page](https://quarkus.io/guides/reactive-event-bus#consuming-events)

fore more details on the consumer implementation there is an event log consumer available in [LogEvents.java](https://github.com/spaship/operator/blob/main/service/src/main/java/io/spaship/operator/event/consumer/LogEvents.java) 
