package io.spaship.operator.rest.website;

import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.spaship.content.git.config.GitContentUtils;
import io.spaship.operator.config.model.WebsiteConfig;
import io.spaship.operator.content.ContentController;
import io.spaship.operator.controller.WebsiteRepository;
import io.spaship.operator.crd.Website;
import io.spaship.operator.rest.website.model.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
@RouteBase(path = WebsiteResource.CONTEXT, produces = MediaType.APPLICATION_JSON)
@RolesAllowed(WebsiteResource.ROLE_SPASHIP_USER)
public class WebsiteResource {

    public static final String CONTEXT = "api/v1/website";
    public static final String API_COMPONENT = CONTEXT + "/component/search?namespace={namespace}&website={website}&env={env}";
    public static final String API_COMPONENT_DETAIL = CONTEXT + "/component/info?namespace={namespace}&website={website}&env={env}&name={component_name}";

    public static final String ROLE_SPASHIP_USER = "spaship-user";

    private static final Logger log = Logger.getLogger(WebsiteResource.class);

    @Inject
    WebsiteRepository websiteRepository;

    @Inject
    ContentController contentController;

    @ConfigProperty(name = "app.operator.url")
    Optional<String> operatorUrl;

    public static List<String> apis(String rootPath) {
        List<String> apis = new ArrayList<>();
        apis.add(rootPath + CONTEXT + "/search?namespace={namespace}&name={name}");
        apis.add(rootPath + API_COMPONENT);
        apis.add(rootPath + API_COMPONENT_DETAIL);
        return apis;
    }

    @Route(methods = HttpMethod.GET, path = "search")
    @Operation(summary = "Website Search", description = "Search websites based on optional parameters name and namespace. Always returns 200")
    @APIResponse(responseCode = "200",
            description = "OK Response with websites",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = WebsiteSearchResponse.class))
    )
    public void search(RoutingContext rc, @Param("namespace") Optional<String> namespace, @Param("name") Optional<String> name) {
        log.tracef("website search. namespace=%s name=%s", namespace, name);

        List<WebsiteResponse> data = websiteRepository.searchWebsite(namespace, name)
                .map(crd -> WebsiteResponse.createFromCrd(crd, operatorUrl))
                .collect(Collectors.toList());

        WebsiteSearchResponse response = WebsiteSearchResponse.success();
        response.setData(data);

        rc.response().end(JsonObject.mapFrom(response).toBuffer());
    }

    @Route(methods = HttpMethod.GET, path = "component/search")
    @Operation(summary = "Component Search", description = "All website components for given environment. Always returns 200")
    @APIResponse(responseCode = "200",
            description = "OK Response with components",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ComponentSearchResponse.class))
    )
    public void components(RoutingContext rc, @NotNull @Param("namespace") String namespace, @NotNull @Param("website") String website, @NotNull @Param("env") String env, @Param("name") Optional<String> name) {
        log.tracef("component search. namespace=%s website=%s env=%s name=%s", namespace, website, env, name);

        // bean validation is not enabled to gracefully validate input params
        if (StringUtils.isAnyEmpty(namespace, website, env)) {
            rc.response().setStatusCode(400).end("input parameters namespace, website, env are required");
            return;
        }

        Optional<Website> web = websiteRepository.searchWebsite(Optional.of(namespace), Optional.of(website)).findFirst();
        if (web.isEmpty()) {
            rc.response().end(JsonObject.mapFrom(ComponentSearchResponse.success(new ArrayList<>())).toBuffer());
            return;
        }
        WebsiteConfig websiteConfig = web.get().getConfig();

        List<Component> data = websiteConfig.getEnabledComponents(env)
                .filter(component -> {
                    if (name.isEmpty() || StringUtils.isEmpty(name.get())) {
                        return true;
                    }
                    return StringUtils.equals(name.get(), contentController.getComponentDirName(component));
                })
                .map(component -> {
                    Component result = new Component();
                    String compName = contentController.getComponentDirName(component);
                    result.setName(compName);
                    result.setPath(component.getContext());
                    result.setRef(GitContentUtils.getRef(websiteConfig, env, component.getContext()));
                    operatorUrl.ifPresent(s -> result.setApi(s + String.format("/api/v1/website/component/info?namespace=%s&website=%s&env=%s&name=%s", namespace, website, env, compName)));
                    return result;
                })
                .collect(Collectors.toList());

        rc.response().end(JsonObject.mapFrom(ComponentSearchResponse.success(data)).toBuffer());
    }

    @Route(methods = HttpMethod.GET, path = "component/info")
    @Operation(summary = "Component Detail", description = "Website component detail - get actual data via Content API")
    @APIResponse(responseCode = "200",
            description = "OK Response with component detail",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ComponentDetailResponse.class))
    )
    @APIResponse(responseCode = "404", description = "Component not exists")
    public void componentInfo(RoutingContext rc, @NotNull @Param("namespace") String namespace, @NotNull @Param("website") String website, @NotNull @Param("env") String env, @NotNull @Param("name") String name) {
        log.tracef("component search. namespace=%s website=%s, env=%s, name=%s", namespace, website, env, name);

        // bean validation is not enabled to gracefully validate input params
        if (StringUtils.isAnyEmpty(namespace, website, env, name)) {
            rc.response().setStatusCode(400).end("input parameters namespace, website, env, name are required");
            return;
        }

        Optional<Website> web = websiteRepository.searchWebsite(Optional.of(namespace), Optional.of(website)).findFirst();
        if (web.isEmpty()) {
            rc.response().setStatusCode(404).end();
            return;
        }

        contentController.componentInfo(web.get(), env, name)
                .onFailure(cause -> {
                    if (cause instanceof NotFoundException) {
                        rc.response().setStatusCode(404).end(cause.toString());
                    } else {
                        rc.response().setStatusCode(503).end(cause.toString());
                    }
                })
                .onSuccess(info -> {
                    ComponentDetail data = ComponentDetail.createFrom(info);
                    data.setName(name);

                    ComponentDetailResponse response = ComponentDetailResponse.success(data);

                    rc.response().end(JsonObject.mapFrom(response).toBuffer());
                });
    }
}
