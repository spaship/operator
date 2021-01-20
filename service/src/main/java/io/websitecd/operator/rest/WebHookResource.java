package io.websitecd.operator.rest;

import io.smallrye.mutiny.Uni;
import io.websitecd.operator.webhook.WebhookService;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/api/webhook/")
@Consumes(MediaType.APPLICATION_JSON)
public class WebHookResource {

    public static final String CONTEXT = "api/webhook/";

    private static final Logger log = Logger.getLogger(WebHookResource.class);

    @Inject
    WebhookService webhookService;

    public static List<String> apis(String rootPath) {
        List<String> apis = new ArrayList<>();
        apis.add(rootPath + CONTEXT);
        return apis;
    }

    @POST
    @Path("")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> webHook(@Context HttpRequest request, String data) throws Exception {
        log.infof("webhook called from url=%s", request.getRemoteHost());
        WebhookService.GIT_PROVIDER provider = webhookService.gitProvider(request);
        if (provider == null) {
            throw new BadRequestException("Unknown provider");
        }
        switch (provider) {
            case GITLAB:
                return webhookService.handleGitlab(request, data);

        }
        throw new BadRequestException("Unknown provider");
    }


}
