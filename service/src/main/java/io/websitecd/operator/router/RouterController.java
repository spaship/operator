package io.websitecd.operator.router;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.openshift.api.model.*;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.websitecd.operator.Utils;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class RouterController {

    private static final Logger log = Logger.getLogger(RouterController.class);

    @Inject
    DefaultOpenShiftClient client;

    @ConfigProperty(name = "app.operator.website.domain")
    protected String domain;

    public void updateWebsiteRoutes(String targetEnv, String namespace, WebsiteConfig config) {
        final String hostSuffix = "-" + namespace + "." + domain;
        final String websiteName = Utils.getWebsiteName(config);
        final String host = websiteName + "-" + targetEnv + hostSuffix;
        // TODO: It's not needed to create all routes for sub pathes when root path is present
        for (ComponentConfig component : config.getComponents()) {
            String context = component.getContext();
            String sanityContext = context.replace("/", "").replace("_", "");

            RouteTargetReferenceBuilder targetReference = new RouteTargetReferenceBuilder().withKind("Service").withWeight(100);
            RoutePortBuilder routePortBuilder = new RoutePortBuilder();
            if (component.isKindGit()) {
                targetReference.withName(websiteName + "-content-" + targetEnv);
                routePortBuilder.withTargetPort(new IntOrString("http"));
            } else {
                targetReference.withName(component.getSpec().getServiceName());
                routePortBuilder.withTargetPort(new IntOrString(component.getSpec().getTargetPort()));
            }

            RouteSpecBuilder spec = new RouteSpecBuilder()
                    .withHost(host)
                    .withPath(context)
                    .withTo(targetReference.build())
                    .withPort(routePortBuilder.build())
                    .withTls(new TLSConfigBuilder().withNewTermination("edge").withInsecureEdgeTerminationPolicy("Redirect").build());

            String name = getRouteName(websiteName, sanityContext, targetEnv);
            RouteBuilder builder = new RouteBuilder()
                    .withMetadata(new ObjectMetaBuilder().withName(name).withLabels(Utils.defaultLabels(targetEnv)).build())
                    .withSpec(spec.build());

            Route route = builder.build();
            log.infof("Deploying route=%s", route.getMetadata().getName());

            client.inNamespace(namespace).routes().createOrReplace(route);
        }

        updateWebsiteInfoRoute(namespace, websiteName, targetEnv, host);
    }

    public void updateWebsiteInfoRoute(String namespace, String websiteName, String targetEnv, String host) {
        final String context = "/websiteinfo";

        RouteTargetReferenceBuilder targetReference = new RouteTargetReferenceBuilder().withKind("Service").withWeight(100);
        RoutePortBuilder routePortBuilder = new RoutePortBuilder();
        targetReference.withName(websiteName + "-content-" + targetEnv);
        routePortBuilder.withTargetPort(new IntOrString("http-api"));

        RouteSpecBuilder spec = new RouteSpecBuilder()
                .withHost(host)
                .withPath(context)
                .withTo(targetReference.build())
                .withPort(routePortBuilder.build())
                .withTls(new TLSConfigBuilder().withNewTermination("edge").withInsecureEdgeTerminationPolicy("Redirect").build());

        String name = getRouteName(websiteName, "websiteinfo", targetEnv);

        RouteBuilder builder = new RouteBuilder()
                .withMetadata(new ObjectMetaBuilder().withName(name).withLabels(Utils.defaultLabels(targetEnv)).build())
                .withSpec(spec.build());

        Route route = builder.build();
        log.infof("Deploying route=%s", route.getMetadata().getName());

        client.inNamespace(namespace).routes().createOrReplace(route);
    }

    public String getRouteName(String websiteName, String sanityContext, String env) {
        StringBuilder routeName = new StringBuilder(websiteName + "-");
        routeName.append(env);
        if (StringUtils.isNotEmpty(sanityContext)) {
            routeName.append("-" + sanityContext);
        }

        return routeName.toString();
    }

}
