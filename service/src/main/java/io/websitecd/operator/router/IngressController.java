package io.websitecd.operator.router;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.*;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.websitecd.operator.Utils;
import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class IngressController {

    private static final Logger log = Logger.getLogger(IngressController.class);

    @Inject
    DefaultOpenShiftClient client;

    @ConfigProperty(name = "app.operator.website.domain")
    protected Optional<String> domain;

    public void updateIngress(String targetEnv, String namespace, WebsiteConfig config) {
        if (domain.isEmpty()) {
            log.infof("No Ingress created. Missing domain configuration.");
            return;
        }
        final String hostSuffix = "-" + namespace + "." + domain.get();
        final String websiteName = Utils.getWebsiteName(config);
        final String host = websiteName + "-" + targetEnv + hostSuffix;
        final String contentService = RouterController.getContentServiceName(websiteName, targetEnv);

        List<HTTPIngressPath> paths = new ArrayList<>();
        for (ComponentConfig c : config.getComponents()) {
            String context = c.getContext();
            if (!OperatorConfigUtils.isComponentEnabled(config, targetEnv, context)) {
                continue;
            }

            IngressBackendBuilder ingressBackendBuilder = new IngressBackendBuilder();

            if (c.isKindGit()) {
                ingressBackendBuilder.withService(new IngressServiceBackendBuilder()
                        .withName(contentService)
                        .withPort(new ServiceBackendPortBuilder().withName("http").build())
                        .build());
            } else if (c.isKindService()) {
                ingressBackendBuilder.withService(new IngressServiceBackendBuilder()
                        .withName(c.getSpec().getServiceName())
                        .withPort(new ServiceBackendPortBuilder().withNumber(c.getSpec().getTargetPort()).build())
                        .build());
            }

            HTTPIngressPath path = new HTTPIngressPath();
            path.setPath(context);
            path.setPathType("Prefix");
            path.setBackend(ingressBackendBuilder.build());
            paths.add(path);
        }

        //websiteinfo
        IngressBackend contentApibackend = new IngressBackendBuilder().withService(new IngressServiceBackendBuilder()
                .withName(contentService)
                .withPort(new ServiceBackendPortBuilder().withName("http-api").build())
                .build()).build();

        HTTPIngressPath path = new HTTPIngressPath();
        path.setPath("/websiteinfo");
        path.setPathType("Prefix");
        path.setBackend(contentApibackend);
        paths.add(path);

        IngressRule rule = new IngressRuleBuilder().withHost(host).withNewHttp().withPaths(paths).endHttp().build();

        String name = RouterController.getRouteName(websiteName, null, targetEnv);

        IngressBuilder builder = new IngressBuilder()
                .withMetadata(new ObjectMetaBuilder().withName(name).withLabels(Utils.defaultLabels(targetEnv, config)).build())
                .withSpec(new IngressSpecBuilder().withRules(rule).build());

        Ingress ingress = builder.build();
        log.infof("Ingress: %s", ingress);

        client.inNamespace(namespace).network().v1().ingresses().createOrReplace(ingress);
    }

}
