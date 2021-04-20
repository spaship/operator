package io.spaship.operator.router;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.*;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.quarkus.runtime.StartupEvent;
import io.spaship.operator.Utils;
import io.spaship.operator.config.model.ComponentConfig;
import io.spaship.operator.config.model.WebsiteConfig;
import io.spaship.operator.crd.Website;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.spaship.operator.config.matcher.ComponentKindMatcher.ComponentGitMatcher;
import static io.spaship.operator.config.matcher.ComponentKindMatcher.ComponentServiceMatcher;

@ApplicationScoped
public class IngressController {

    private static final Logger log = Logger.getLogger(IngressController.class);

    @Inject
    DefaultOpenShiftClient client;

    @ConfigProperty(name = "app.operator.website.domain")
    protected Optional<String> domain;

    @ConfigProperty(name = "app.operator.router.mode")
    String routerMode;

    void startup(@Observes StartupEvent event) {
        log.infof("IngressController enabled=%s", isEnabled());
    }

    public boolean isEnabled() {
        return routerMode.equals("ingress");
    }

    public String getContentHost(String targetEnv, Website website) {
        if (domain.isEmpty()) {
            return null;
        }
        final String hostSuffix = "-" + website.getMetadata().getNamespace() + "." + domain.get();
        final String websiteName = Utils.getWebsiteName(website);
        return websiteName + "-" + targetEnv + hostSuffix;
    }

    public Ingress updateIngress(String targetEnv, Website website) {
        if (domain.isEmpty()) {
            log.infof("No Ingress created. Missing domain configuration.");
            return null;
        }
        String namespace = website.getMetadata().getNamespace();
        WebsiteConfig config = website.getConfig();
        final String websiteName = Utils.getWebsiteName(website);
        final String host = getContentHost(targetEnv, website);
        final String contentService = RouterController.getContentServiceName(websiteName, targetEnv);

        List<HTTPIngressPath> paths = new ArrayList<>();
        config.getEnabledComponents(targetEnv)
                .map(component -> createIngressPath(component, contentService))
                .forEach(paths::add);

        IngressRule rule = new IngressRuleBuilder().withHost(host).withNewHttp().withPaths(paths).endHttp().build();

        String name = RouterController.getRouteName(websiteName, null, targetEnv);

        IngressBuilder builder = new IngressBuilder()
                .withMetadata(new ObjectMetaBuilder().withName(name).withLabels(Utils.defaultLabels(targetEnv, website)).build())
                .withSpec(new IngressSpecBuilder().withRules(rule).build());

        Ingress ingress = builder.build();

        log.tracef("Ingress: %s", ingress);
        Ingress created = client.inNamespace(namespace).network().v1().ingresses().createOrReplace(ingress);
        return created;
    }

    public HTTPIngressPath createIngressPath(ComponentConfig c, String contentService) {
        IngressBackendBuilder ingressBackendBuilder = new IngressBackendBuilder();

        if (ComponentGitMatcher.test(c)) {
            ingressBackendBuilder.withService(new IngressServiceBackendBuilder()
                    .withName(contentService)
                    .withPort(new ServiceBackendPortBuilder().withName("http").build())
                    .build());
        } else if (ComponentServiceMatcher.test(c)) {
            ingressBackendBuilder.withService(new IngressServiceBackendBuilder()
                    .withName(c.getSpec().getServiceName())
                    .withPort(new ServiceBackendPortBuilder().withNumber(c.getSpec().getTargetPort()).build())
                    .build());
        }
        HTTPIngressPath path = new HTTPIngressPath();
        path.setPath(c.getContext());
        path.setPathType("Prefix");
        path.setBackend(ingressBackendBuilder.build());

        return path;
    }

    public void deleteIngress(String targetEnv, Website website) {
        String namespace = website.getMetadata().getNamespace();
        final String websiteName = Utils.getWebsiteName(website);
        String name = RouterController.getRouteName(websiteName, null, targetEnv);

        client.inNamespace(namespace).network().v1().ingresses().withName(name).delete();
    }

}
