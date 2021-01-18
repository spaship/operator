package io.websitecd.operator.rest;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class ApiRootResource {

    @ConfigProperty(name = "quarkus.http.root-path")
    String rootPath;

    @GET
    @Path("")
    public List<String> apis() {
        List<String> apis = WebHookResource.apis(rootPath);
        return apis;
    }

}
