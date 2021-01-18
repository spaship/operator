package io.websitecd.operator.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/api/webhook/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WebHookResource {

    public static final String CONTEXT = "api/webhook/";


    public static List<String> apis(String rootPath) {
        List<String> apis = new ArrayList<>();
        apis.add(rootPath + CONTEXT);
        return apis;
    }

    @POST
    @Path("")
    public String websiteHook() {
        return "DONE";
    }


}
