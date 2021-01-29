package io.websitecd.operator.router;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.openshift.api.model.*;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.websitecd.operator.Utils;
import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
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

    public void updateWebsiteRoutes(String targetEnv, String namespace, WebsiteConfig config) {
        if (domain.isEmpty()) {
            log.infof("No Router created. Missing domain configuration.");
            return;
        }
        final String hostSuffix = "-" + namespace + "." + domain.get();
        final String websiteName = Utils.getWebsiteName(config);
        final String host = websiteName + "-" + targetEnv + hostSuffix;
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
                    .withHost(host)
                    .withPath(context)
                    .withTo(targetReference.build())
                    .withPort(routePortBuilder.build())
                    .withTls(new TLSConfigBuilder().withNewTermination("edge").withInsecureEdgeTerminationPolicy("Redirect").build());

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

    public static IntOrString getIntOrString(String s) {
        if (NumberUtils.isParsable(s)) {
            return new IntOrString(NumberUtils.toInt(s));
        } else {
            return new IntOrString(s);
        }
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
                .withHost(host)
                .withPath(context)
                .withTo(targetReference.build())
                .withPort(routePortBuilder.build())
                .withTls(new TLSConfigBuilder().withNewTermination("edge").withInsecureEdgeTerminationPolicy("Redirect").build());

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

}
