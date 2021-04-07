package io.spaship.operator.rest;

import io.spaship.operator.controller.WebsiteController;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/health")
@Produces(MediaType.TEXT_PLAIN)
public class HealthCheckResource {

    @Inject
    WebsiteController websiteController;

    @GET
    @Path("live")
    @Operation(summary = "Liveness check")
    @APIResponse(responseCode = "200", content = @Content(mediaType = MediaType.TEXT_PLAIN, example = "live"))
    public String live() {
        return "live";
    }

    @GET
    @Path("ready")
    @Operation(summary = "Readiness check", description = "Check if Website CRD Controller is ready to manage CRDs (if enabled)")
    @APIResponse(responseCode = "200", content = @Content(mediaType = MediaType.TEXT_PLAIN, example = "ready"))
    @APIResponse(responseCode = "204", content = @Content(mediaType = MediaType.TEXT_PLAIN))
    public Response ready() {
        if (websiteController.isReady()) {
            return Response.ok("ready").build();
        }
        return Response.noContent().build();
    }

}
