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
import java.util.Optional;

@ApplicationScoped
public class RouterController {

    private static final Logger log = Logger.getLogger(RouterController.class);

    @Inject
    DefaultOpenShiftClient client;

    @ConfigProperty(name = "app.operator.website.domain")
    protected Optional<String> domain;

    public void updateWebsiteRoutes(String targetEnv, Website website) {
        String namespace = website.getMetadata().getNamespace();
        WebsiteConfig config = website.getConfig();
        final String websiteName = Utils.getWebsiteName(website);

        String host = null;
        if (domain.isPresent()) {
            final String hostSuffix = "-" + namespace + "." + domain.get();
            host = "http://" + websiteName + "-" + targetEnv + hostSuffix;
        }

        // TODO: It's not needed to create all routes for sub pathes when root path is present

        for (ComponentConfig component : config.getComponents()) {
            String context = component.getContext();
            if (!OperatorConfigUtils.isComponentEnabled(config, targetEnv, context)) {
                continue;
            }

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

        updateWebsiteInfoRoute(namespace, websiteName, targetEnv, host, config);
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

    public void updateWebsiteInfoRoute(String namespace, String websiteName, String targetEnv, String host, WebsiteConfig config) {
        final String context = "/websiteinfo";

        RouteTargetReferenceBuilder targetReference = new RouteTargetReferenceBuilder().withKind("Service").withWeight(100);
        RoutePortBuilder routePortBuilder = new RoutePortBuilder();
        targetReference.withName(getContentServiceName(websiteName, targetEnv));
        routePortBuilder.withTargetPort(new IntOrString("http-api"));

        RouteSpecBuilder spec = new RouteSpecBuilder()
                .withPath(context)
                .withTo(targetReference.build())
                .withPort(routePortBuilder.build())
                .withTls(new TLSConfigBuilder().withNewTermination("edge").withInsecureEdgeTerminationPolicy("Redirect").build());

        if (StringUtils.isNotEmpty(host)) {
            spec.withHost(host);
        }

        String name = getRouteName(websiteName, "websiteinfo", targetEnv);

        RouteBuilder builder = new RouteBuilder()
                .withMetadata(new ObjectMetaBuilder().withName(name).withLabels(Utils.defaultLabels(targetEnv, config)).build())
                .withSpec(spec.build());

        Route route = builder.build();
        log.infof("Deploying route=%s", route.getMetadata().getName());

        client.inNamespace(namespace).routes().createOrReplace(route);
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

        for (ComponentConfig component : website.getConfig().getComponents()) {
            String sanityContext = sanityContext(component.getContext());
            String name = getRouteName(websiteName, sanityContext, targetEnv);
            client.inNamespace(namespace).routes().withName(name).delete();
        }
    }

}
