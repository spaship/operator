package io.websitecd.operator.rest;

import io.websitecd.operator.controller.WebsiteController;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/health")
public class HealthCheckResource {

    @Inject
    WebsiteController websiteController;

    @GET
    @Path("live")
    public String live() {
        return "live";
    }

    @GET
    @Path("ready")
    public Response ready() {
        if (websiteController.isReady()) {
            return Response.ok("ready").build();
        }
        return Response.noContent().build();
    }

}
