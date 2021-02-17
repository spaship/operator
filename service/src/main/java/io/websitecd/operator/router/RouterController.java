package io.websitecd.operator.router;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.openshift.api.model.*;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.websitecd.operator.Utils;
import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.crd.Website;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class RouterController {

    private static final Logger log = Logger.getLogger(RouterController.class);

    @Inject
    DefaultOpenShiftClient client;

    @ConfigProperty(name = "app.operator.website.domain")
    protected Optional<String> domain;

    final String API_ROUTE_NAME = "api";

    public Set<ComponentConfig> getComponents(WebsiteConfig config, String targetEnv) {
        Set<ComponentConfig> enabledComponents = config.getComponents().stream()
                .filter(component -> OperatorConfigUtils.isComponentEnabled(config, targetEnv, component.getContext()))
                .collect(Collectors.toSet());

        ComponentConfig rootComponent = null;
        for (ComponentConfig c : enabledComponents) {
            if (c.getContext().equals("/") && c.isKindGit()) {
                rootComponent = c;
                break;
            }
        }
        if (rootComponent != null) {
            log.infof("Root component found. Working only with root context");
            enabledComponents = new HashSet<>(1);
            enabledComponents.add(rootComponent);
        }
        log.tracef("enabled components=%s", enabledComponents);
        return enabledComponents;
    }

    public void updateWebsiteRoutes(String targetEnv, Website website) {
        String namespace = website.getMetadata().getNamespace();
        WebsiteConfig config = website.getConfig();
        final String websiteName = Utils.getWebsiteName(website);

        String host = null;
        if (domain.isPresent()) {
            final String hostSuffix = "-" + namespace + "." + domain.get();
            host = websiteName + "-" + targetEnv + hostSuffix;
        }

        Set<ComponentConfig> enabledComponents = getComponents(config, targetEnv);

        for (ComponentConfig component : enabledComponents) {
            String context = component.getContext();

            RouteTargetReferenceBuilder targetReference = new RouteTargetReferenceBuilder().withKind("Service").withWeight(100);
            RoutePortBuilder routePortBuilder = new RoutePortBuilder();
            if (component.isKindGit()) {
                targetReference.withName(getContentServiceName(websiteName, targetEnv));
                routePortBuilder.withTargetPort(new IntOrString("http"));
            } else {
                targetReference.withName(component.getSpec().getServiceName());
                routePortBuilder.withTargetPort(getIntOrString(component.getSpec().getTargetPort()));
            }
            RouteSpecBuilder spec = new RouteSpecBuilder()
                    .withPath(context)
                    .withTo(targetReference.build())
                    .withPort(routePortBuilder.build())
                    .withTls(new TLSConfigBuilder().withNewTermination("edge").withInsecureEdgeTerminationPolicy("Redirect").build());

            if (domain.isPresent()) {
                spec.withHost(host);
            }

            String sanityContext = sanityContext(context);
            String name = getRouteName(websiteName, sanityContext, targetEnv);
            RouteBuilder builder = new RouteBuilder()
                    .withMetadata(new ObjectMetaBuilder().withName(name).withLabels(Utils.defaultLabels(targetEnv, config)).build())
                    .withSpec(spec.build());

            Route route = builder.build();
            log.infof("Deploying route=%s", route.getMetadata().getName());

            client.inNamespace(namespace).routes().createOrReplace(route);
        }
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

    public Route updateApiRoute(String targetEnv, Website website) {
        final String websiteName = Utils.getWebsiteName(website);

        RouteTargetReferenceBuilder targetReference = new RouteTargetReferenceBuilder().withKind("Service").withWeight(100);
        RoutePortBuilder routePortBuilder = new RoutePortBuilder();
        targetReference.withName(getContentServiceName(websiteName, targetEnv));
        routePortBuilder.withTargetPort(new IntOrString("http-api"));

        RouteSpecBuilder specBuilder = new RouteSpecBuilder()
                .withTo(targetReference.build())
                .withPort(routePortBuilder.build());

        String name = getRouteName(websiteName, API_ROUTE_NAME, targetEnv);

        RouteBuilder builder = new RouteBuilder()
                .withMetadata(new ObjectMetaBuilder().withName(name).withLabels(Utils.defaultLabels(targetEnv, website.getConfig())).build())
                .withSpec(specBuilder.build());

        Route route = builder.build();
        log.infof("Deploying route=%s", route.getMetadata().getName());

        return client.inNamespace(website.getMetadata().getNamespace()).routes().createOrReplace(route);
    }

    public static String getRouteName(String websiteName, String sanityContext, String env) {
        StringBuilder routeName = new StringBuilder(websiteName + "-");
        routeName.append(env);
        if (StringUtils.isNotEmpty(sanityContext)) {
            routeName.append("-" + sanityContext);
        }

        return routeName.toString();
    }

    public void deleteWebsiteRoutes(String targetEnv, Website website) {
        String namespace = website.getMetadata().getNamespace();
        final String websiteName = Utils.getWebsiteName(website);
        WebsiteConfig config = website.getConfig();

        Set<ComponentConfig> enabledComponents = getComponents(config, targetEnv);

        for (ComponentConfig component : enabledComponents) {
            String sanityContext = sanityContext(component.getContext());
            String name = getRouteName(websiteName, sanityContext, targetEnv);
            client.inNamespace(namespace).routes().withName(name).delete();
        }
        String name = getRouteName(websiteName, API_ROUTE_NAME, targetEnv);
        client.inNamespace(namespace).routes().withName(name).delete();
    }

}
