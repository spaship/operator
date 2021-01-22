package io.websitecd.operator.openshift;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.websitecd.operator.Utils;
import io.websitecd.operator.config.model.Environment;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.router.RouterController;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class OperatorService {

    private static final Logger log = Logger.getLogger(OperatorService.class);

    @ConfigProperty(name = "app.operator.website.url")
    String gitUrl;

    @ConfigProperty(name = "app.operator.namespace")
    protected Optional<String> namespace;

    @ConfigProperty(name = "app.operator.init.delay")
    protected long initDelay;

    @Inject
    ContentController contentController;

    @Inject
    WebsiteConfigService websiteConfigService;

    @Inject
    RouterController routerController;

    @Inject
    DefaultOpenShiftClient client;

    @Inject
    Vertx vertx;

    Map<String, WebsiteConfig> websites = new HashMap<>();

    void onStart(@Observes StartupEvent ev) {
        log.infof("Registering INIT with delay=%s", initDelay);
        vertx.setTimer(initDelay, e -> {
            vertx.executeBlocking(future -> {
                initServices();
                future.complete();
            }, res -> log.infof("Initialization completed"));
        });
    }

    public void initServices() {
        log.infof("Init service. openshift_url=%s", client.getOpenshiftUrl());

        try {
            WebsiteConfig config = websiteConfigService.cloneRepo(gitUrl);

            websites.put(gitUrl, config);

            processConfig(gitUrl);
        } catch (Exception e) {
            log.error("Cannot init core services", e);
        }
    }

    public void processConfig(String gitUrl) {
        WebsiteConfig config = websites.get(gitUrl);
        Map<String, Environment> envs = config.getEnvs();
        for (Map.Entry<String, Environment> envEntry : envs.entrySet()) {
            if (!namespace.isEmpty() && !envEntry.getValue().getNamespace().equals(namespace.get())) {
                log.infof("namespace ignored name=%s", namespace);
                continue;
            }
            // ? create namespace ???
            String env = envEntry.getKey();
            contentController.createClient(gitUrl, env, config);

            setupCoreServices(env, config);
        }
    }

//    public void createNamespaces(String prefix, List<String> envs) {
//        for (String env : envs) {
//            String name = getNameSpaceName(prefix, env);
//            Namespace ns = new NamespaceBuilder().withNewMetadata().withName(name).addToLabels("app", nameSpaceLabelValue).endMetadata().build();
//            client.namespaces().withName(name).createOrReplace(ns);
//            log.infof("Namespace created. name=%s", name);
//        }
//    }

    public void setupCoreServices(String env, WebsiteConfig config) {
        String namespace = config.getEnvironment(env).getNamespace();
        final String websiteName = Utils.getWebsiteName(config);
        log.infof("Create core services websiteName=%s env=%s namespace=%s", websiteName, env, namespace);
        contentController.updateConfigs(env, namespace, config);
        contentController.deploy(env, namespace, websiteName);

        routerController.updateWebsiteRoutes(env, namespace, config);
    }

    /**
     * Webiste. Key = giturl, Value = config
     *
     * @return
     */
    public Map<String, WebsiteConfig> getWebsites() {
        return websites;
    }

    public boolean isKnownWebsite(String gitUrl) {
        return websites.containsKey(gitUrl);
    }
}
