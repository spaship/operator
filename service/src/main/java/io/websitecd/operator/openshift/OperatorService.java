package io.websitecd.operator.openshift;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.websitecd.operator.Utils;
import io.websitecd.operator.config.model.Environment;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.router.IngressController;
import io.websitecd.operator.router.RouterController;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class OperatorService {

    private static final Logger log = Logger.getLogger(OperatorService.class);

    @ConfigProperty(name = "app.operator.website.url")
    String gitUrl;

    @ConfigProperty(name = "app.operator.website.branch")
    String branch;

    @ConfigProperty(name = "app.operator.namespace")
    protected Optional<String> namespace;

    @ConfigProperty(name = "app.operator.init.delay")
    protected long initDelay;

    @ConfigProperty(name = "app.operator.router.mode")
    String routerMode;

    @Inject
    ContentController contentController;

    @Inject
    WebsiteConfigService websiteConfigService;

    @Inject
    RouterController routerController;

    @Inject
    IngressController ingressController;

    @Inject
    DefaultOpenShiftClient client;

    @Inject
    Vertx vertx;

    void onStart(@Observes StartupEvent ev) {
        log.infof("Registering INIT with delay=%s", initDelay);
        vertx.setTimer(initDelay, e -> {
            vertx.executeBlocking(future -> {
                try {
                    initServices(gitUrl, branch);
                } catch (Exception ex) {
                    future.fail(ex);
                }
                future.complete();
            }, res -> log.infof("Initialization completed. success=%s", !res.failed()));
        });
    }

    public void initServices(String gitUrl, String branch) throws IOException, GitAPIException, URISyntaxException {
        log.infof("Init service. openshift_url=%s", client.getOpenshiftUrl());

        try {
            websiteConfigService.cloneRepo(gitUrl, branch);

            processConfig(gitUrl, false, true);
        } catch (Exception e) {
            log.error("Cannot init core services for gitUrl=" + gitUrl, e);
            throw e;
        }
    }

    public boolean isEnvEnabled(Environment env) {
        if (namespace.isEmpty()) {
            return true;
        }
        return env.getNamespace().equals(namespace.get());
    }

    public void processConfig(String gitUrl, boolean redeploy, boolean createClients) {
        WebsiteConfig config = websiteConfigService.getConfig(gitUrl);
        Map<String, Environment> envs = config.getEnvs();
        for (Map.Entry<String, Environment> envEntry : envs.entrySet()) {
            if (!isEnvEnabled(envEntry.getValue())) {
                log.infof("namespace ignored name=%s", namespace);
                continue;
            }
            // ? create namespace ???
            String env = envEntry.getKey();
            log.infof("Processing env=%s", env);
            if (createClients) {
                contentController.createClient(gitUrl, env, config);
            }

            // TODO: Create it in working thread or async
            setupCoreServices(env, config);
            if (redeploy) {
                contentController.redeploy(env, config);
            }
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
        contentController.deploy(env, namespace, websiteName, config);

        if (StringUtils.equals(routerMode, "ingress")) {
            ingressController.updateIngress(env, namespace, config);
        } else if (StringUtils.equals(routerMode, "openshift")) {
            routerController.updateWebsiteRoutes(env, namespace, config);
        } else {
            log.infof("No routing created");
        }
    }

}
