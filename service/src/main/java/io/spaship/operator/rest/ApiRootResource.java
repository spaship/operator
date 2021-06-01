package io.spaship.operator.rest;

import io.spaship.operator.event.EventSourcingEngine;
import io.spaship.operator.rest.website.WebsiteResource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import java.util.List;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class ApiRootResource {


    private final String rootPath;
    private final EventSourcingEngine eventSourcingEngine;

    public ApiRootResource(EventSourcingEngine eventSourcingEngine, @ConfigProperty(name = "quarkus.http.root-path")
            String property) {
        this.eventSourcingEngine = eventSourcingEngine;
        this.rootPath = property;
    }


    @GET
    @Path("")
    @Operation(summary = "List of APIs", description = "Shows available Operator's APIs")
    @APIResponse(
            responseCode = "200", description = "List of API URLs",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, example = "[\"/api/webhook/\",\"/api/v1/website/" +
                    "search\",\"/api/v1/website/{namespace}/{website}/{env}/component\",\"/api/v1/website/{namespace}/" +
                    "{website}/{env}/component/{name}\"]")
    )
    public List<String> apis() {
        eventSourcingEngine.publishMessage("invoked /api endpoint");
        List<String> apis = WebHookResource.apis(rootPath);
        apis.addAll(WebsiteResource.apis(rootPath));
        return apis;
    }

}
