package io.spaship.operator.router;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.openshift.api.model.*;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.quarkus.runtime.StartupEvent;
import io.spaship.operator.Utils;
import io.spaship.operator.config.model.ComponentConfig;
import io.spaship.operator.config.model.WebsiteConfig;
import io.spaship.operator.crd.Website;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static io.spaship.operator.config.matcher.ComponentKindMatcher.ComponentGitMatcher;
import static io.spaship.operator.config.matcher.ComponentKindMatcher.ComponentServiceMatcher;

@ApplicationScoped
public class RouterController {

    private static final Logger log = Logger.getLogger(RouterController.class);

    @Inject
    DefaultOpenShiftClient client;

    @ConfigProperty(name = "app.operator.website.domain")
    protected Optional<String> domain;

    @ConfigProperty(name = "app.operator.router.openshift.api.route")
    String apiRouteName;

    @ConfigProperty(name = "app.operator.router.mode")
    String routerMode;

    void startup(@Observes StartupEvent event) {
        log.infof("RouterController enabled=%s", isEnabled());
    }

    public boolean isEnabled() {
        return routerMode.equals("openshift");
    }

    public Stream<ComponentConfig> getGitComponents(WebsiteConfig config, String targetEnv) {
        Optional<ComponentConfig> rootComponent = config.getEnabledGitComponents(targetEnv)
                .filter(c -> c.getContext().equals("/"))
                .findFirst();

        if (rootComponent.isPresent()) {
            log.infof("Root component found. Working only with root context");
            return Stream.of(rootComponent.get());
        } else {
            return config.getEnabledGitComponents(targetEnv);
        }
    }

    public Stream<ComponentConfig> getServiceComponents(WebsiteConfig config, String targetEnv) {
        return config.getEnabledServiceComponents(targetEnv);
    }

    protected static String getHost(Optional<String> domain, String websiteName, String targetEnv, String namespace) {
        if (domain.isPresent()) {
            return websiteName + "-" + targetEnv + "-" + namespace + "." + domain.get();
        }
        return null;
    }

    public List<Route> updateWebsiteRoutes(String targetEnv, Website website) {
        String namespace = website.getMetadata().getNamespace();
        WebsiteConfig config = website.getConfig();
        final String websiteName = Utils.getWebsiteName(website);

        String finalHost = getHost(domain, websiteName, targetEnv, namespace);

        Map<String, String> defaultLabels = Utils.defaultLabels(targetEnv, website);
        String contentServiceName = getContentServiceName(websiteName, targetEnv);

        List<Route> routes = new ArrayList<>();
        getGitComponents(config, targetEnv)
                .map(component -> createRouteBuilder(component, contentServiceName, finalHost, websiteName, targetEnv, defaultLabels))
                .forEach(builder -> {
                    Route route = builder.build();
                    log.infof("Deploying route=%s kind=git", route.getMetadata().getName());
                    Route r = client.inNamespace(namespace).routes().createOrReplace(route);
                    routes.add(r);
                });
        getServiceComponents(config, targetEnv)
                .map(component -> createRouteBuilder(component, contentServiceName, finalHost, websiteName, targetEnv, defaultLabels))
                .forEach(builder -> {
                    Route route = builder.build();
                    log.infof("Deploying route=%s kind=service", route.getMetadata().getName());
                    Route r = client.inNamespace(namespace).routes().createOrReplace(route);
                    routes.add(r);
                });
        return routes;
    }

    public RouteBuilder createRouteBuilder(ComponentConfig component, String contentServiceName, String host, String websiteName, String targetEnv, Map<String, String> labels) {
        RouteTargetReferenceBuilder targetReference = new RouteTargetReferenceBuilder().withKind("Service").withWeight(100);
        RoutePortBuilder routePortBuilder = new RoutePortBuilder();
        if (ComponentGitMatcher.test(component)) {
            targetReference.withName(contentServiceName);
            routePortBuilder.withTargetPort(new IntOrString("http"));
        } else if (ComponentServiceMatcher.test(component)) {
            targetReference.withName(component.getSpec().getServiceName());
            routePortBuilder.withTargetPort(getIntOrString(component.getSpec().getTargetPort()));
        }
        RouteSpecBuilder spec = new RouteSpecBuilder()
                .withPath(component.getContext())
                .withTo(targetReference.build())
                .withPort(routePortBuilder.build())
                .withTls(new TLSConfigBuilder().withNewTermination("edge").withInsecureEdgeTerminationPolicy("Redirect").build());

        if (StringUtils.isNotEmpty(host)) {
            spec.withHost(host);
        }
        String sanityContext = sanityContext(component.getContext());
        String name = getRouteName(websiteName, sanityContext, targetEnv);
        return new RouteBuilder()
                .withMetadata(new ObjectMetaBuilder().withName(name).withLabels(labels).build())
                .withSpec(spec.build());
    }

    public static IntOrString getIntOrString(Integer i) {
        return new IntOrString(i);
    }

    public static String getContentServiceName(String websiteName, String targetEnv) {
        return websiteName + "-content-" + targetEnv;
    }

    public static String sanityContext(String context) {
        return context.replace("/", "").replace("_", "");
    }

    protected Route createApiRoute(String targetEnv, Website website) {
        final String websiteName = Utils.getWebsiteName(website);
        String namespace = website.getMetadata().getNamespace();

        String host = getHost(domain, websiteName, targetEnv + "-api", namespace);

        RouteTargetReferenceBuilder targetReference = new RouteTargetReferenceBuilder().withKind("Service").withWeight(100);
        RoutePortBuilder routePortBuilder = new RoutePortBuilder();
        targetReference.withName(getContentServiceName(websiteName, targetEnv));
        routePortBuilder.withTargetPort(new IntOrString("http-api"));

        RouteSpecBuilder specBuilder = new RouteSpecBuilder()
                .withTo(targetReference.build())
                .withPort(routePortBuilder.build());

        if (StringUtils.isNotEmpty(host)) {
            specBuilder.withHost(host);
        }

        String name = getRouteName(websiteName, apiRouteName, targetEnv);

        RouteBuilder builder = new RouteBuilder()
                .withMetadata(new ObjectMetaBuilder().withName(name).withLabels(Utils.defaultLabels(targetEnv, website)).build())
                .withSpec(specBuilder.build());

        return builder.build();
    }

    public Route updateApiRoute(String targetEnv, Website website) {
        Route route = createApiRoute(targetEnv, website);
        log.infof("Deploying route=%s", route.getMetadata().getName());

        return client.inNamespace(website.getMetadata().getNamespace()).routes().createOrReplace(route);
    }

    public static String getRouteName(String websiteName, String sanityContext, String env) {
        StringBuilder routeName = new StringBuilder(websiteName + "-");
        routeName.append(env);
        if (StringUtils.isNotEmpty(sanityContext)) {
            routeName.append("-").append(sanityContext);
        }

        return routeName.toString();
    }

    public void deleteWebsiteRoutes(String targetEnv, Website website) {
        String namespace = website.getMetadata().getNamespace();
        final String websiteName = Utils.getWebsiteName(website);
        WebsiteConfig config = website.getConfig();

        config.getEnabledComponents(targetEnv)
                .map(c -> getRouteName(websiteName, sanityContext(c.getContext()), targetEnv))
                .forEach(name -> client.inNamespace(namespace).routes().withName(name).delete());

        String name = getRouteName(websiteName, apiRouteName, targetEnv);
        client.inNamespace(namespace).routes().withName(name).delete();
    }

}
