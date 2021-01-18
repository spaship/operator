package io.websitecd.operator.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/health")
public class HealthCheckResource {

    @GET
    @Path("live")
    public String live() {
        return "live";
    }

    @GET
    @Path("ready")
    public String ready() {
        return "ready";
    }

}
