# Website events


### event lists
<table>
<th>Event Name</th><th>Description</th><th>Is Available</th>
<tr><td>WEBSITE_CREATE</td><td>website create event</td><td>yes</td></tr>
<tr><td>WEBSITE_UPDATE</td><td>website update event</td><td>yes</td></tr>
<tr><td>WEBSITE_DELETE</td><td>website delete event</td><td>yes</td></tr>
<tr><td>PREVIEW_CREATE</td><td>website preview create event</td><td>yes</td></tr>
<tr><td>PREVIEW_UPDATE</td><td>website preview update event</td><td>yes</td></tr>
<tr><td>PREVIEW_DELETE</td><td>website preview delete event</td><td>yes</td></tr>
<table>

### consume events
all the events are published in this bus `crud-event-source`, there are two ways to consume events within the application 
1. `vertx.eventBus().consumer("crud-event-source",handler->...)` for more details on browse [this page](https://vertx.io/docs/vertx-core/java/#_registering_handlers)
2. use `@ConsumeEvent` annotation. for more details on browse [this page](https://quarkus.io/guides/reactive-event-bus#consuming-events)

fore more details on the consumer implementation there is an event log consumer available in [this location](../../service/src/main/java/io/spaship/operator/event/consumer/LogEvents.java) 



